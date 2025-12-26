package main

import (
	"encoding/json"
	"fmt"
	"os"
	"os/exec"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/charmbracelet/bubbles/spinner"
	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/huh"
	"github.com/charmbracelet/lipgloss"
	"github.com/common-nighthawk/go-figure"
)

// ============================================================================
// Styles
// ============================================================================

var (
	// Colors
	primaryColor   = lipgloss.Color("#7C3AED") // Purple
	secondaryColor = lipgloss.Color("#06B6D4") // Cyan
	successColor   = lipgloss.Color("#10B981") // Green
	warningColor   = lipgloss.Color("#F59E0B") // Amber
	errorColor     = lipgloss.Color("#EF4444") // Red
	mutedColor     = lipgloss.Color("#6B7280") // Gray

	// Styles
	titleStyle = lipgloss.NewStyle().
			Bold(true).
			Foreground(primaryColor).
			MarginBottom(1)

	subtitleStyle = lipgloss.NewStyle().
			Foreground(secondaryColor).
			Italic(true)

	successStyle = lipgloss.NewStyle().
			Bold(true).
			Foreground(successColor)

	errorStyle = lipgloss.NewStyle().
			Bold(true).
			Foreground(errorColor)

	warningStyle = lipgloss.NewStyle().
			Bold(true).
			Foreground(warningColor)

	infoStyle = lipgloss.NewStyle().
			Foreground(secondaryColor)

	mutedStyle = lipgloss.NewStyle().
			Foreground(mutedColor)

	boxStyle = lipgloss.NewStyle().
			Border(lipgloss.RoundedBorder()).
			BorderForeground(primaryColor).
			Padding(1, 2).
			MarginTop(1).
			MarginBottom(1)

	stepStyle = lipgloss.NewStyle().
			Foreground(primaryColor).
			Bold(true)

	checkmarkStyle = lipgloss.NewStyle().
			Foreground(successColor).
			Bold(true)

	crossStyle = lipgloss.NewStyle().
			Foreground(errorColor).
			Bold(true)
)

// Custom form theme with better button visibility
func getFormTheme() *huh.Theme {
	t := huh.ThemeBase()
	
	// Make the focused/selected button bright green
	t.Focused.FocusedButton = t.Focused.FocusedButton.
		Background(lipgloss.Color("#10B981")).
		Foreground(lipgloss.Color("#FFFFFF")).
		Bold(true)
	
	// Make the blurred/unselected button dim gray
	t.Focused.BlurredButton = t.Focused.BlurredButton.
		Background(lipgloss.Color("#374151")).
		Foreground(lipgloss.Color("#9CA3AF"))
	
	// Style the title and description
	t.Focused.Title = t.Focused.Title.
		Foreground(primaryColor).
		Bold(true)
	
	t.Focused.Description = t.Focused.Description.
		Foreground(mutedColor)
	
	// Selected option styling
	t.Focused.SelectedOption = t.Focused.SelectedOption.
		Foreground(successColor).
		Bold(true)
	
	t.Focused.UnselectedOption = t.Focused.UnselectedOption.
		Foreground(lipgloss.Color("#D1D5DB"))
	
	return t
}

// ============================================================================
// Types
// ============================================================================

type ReleaseType int

const (
	Patch ReleaseType = iota
	Feature
	Major
)

func (r ReleaseType) String() string {
	switch r {
	case Major:
		return "Major"
	case Feature:
		return "Feature"
	case Patch:
		return "Patch"
	default:
		return "Unknown"
	}
}

func (r ReleaseType) Emoji() string {
	switch r {
	case Major:
		return "🚀"
	case Feature:
		return "✨"
	case Patch:
		return "🐛"
	default:
		return "?"
	}
}

type Version struct {
	Major int
	Minor int
	Patch int
}

func (v Version) String() string {
	return fmt.Sprintf("v%d.%d.%d", v.Major, v.Minor, v.Patch)
}

func (v Version) PomString() string {
	return fmt.Sprintf("%d.%d.%d", v.Major, v.Minor, v.Patch)
}

func (v Version) IsZero() bool {
	return v.Major == 0 && v.Minor == 0 && v.Patch == 0
}

func ParseVersion(s string) (Version, error) {
	s = strings.TrimPrefix(s, "v")
	parts := strings.Split(s, ".")
	if len(parts) != 3 {
		return Version{}, fmt.Errorf("invalid version format: %s", s)
	}

	major, err := strconv.Atoi(parts[0])
	if err != nil {
		return Version{}, err
	}
	minor, err := strconv.Atoi(parts[1])
	if err != nil {
		return Version{}, err
	}
	patch, err := strconv.Atoi(parts[2])
	if err != nil {
		return Version{}, err
	}

	return Version{Major: major, Minor: minor, Patch: patch}, nil
}

func (v Version) Bump(rt ReleaseType) Version {
	switch rt {
	case Major:
		return Version{Major: v.Major + 1, Minor: 0, Patch: 0}
	case Feature:
		return Version{Major: v.Major, Minor: v.Minor + 1, Patch: 0}
	case Patch:
		return Version{Major: v.Major, Minor: v.Minor, Patch: v.Patch + 1}
	default:
		return v
	}
}

type GitHubRelease struct {
	TagName string `json:"tagName"`
}

// ErrorAction represents what the user wants to do when an error occurs
type ErrorAction int

const (
	ActionRetry ErrorAction = iota
	ActionSkip
	ActionRollback
	ActionAbort
)

// ReleaseState tracks the current state of the release process
type ReleaseState struct {
	OriginalPomContent []byte
	PomModified        bool
	ChangesStaged      bool
	ChangesCommitted   bool
	ChangesPushed      bool
	ReleaseCreated     bool
	NewVersion         Version
}

// ============================================================================
// Spinner Model for Running Commands
// ============================================================================

type spinnerModel struct {
	spinner  spinner.Model
	message  string
	done     bool
	success  bool
	quitting bool
}

func newSpinnerModel(message string) spinnerModel {
	s := spinner.New()
	s.Spinner = spinner.Dot
	s.Style = lipgloss.NewStyle().Foreground(primaryColor)
	return spinnerModel{
		spinner: s,
		message: message,
	}
}

func (m spinnerModel) Init() tea.Cmd {
	return m.spinner.Tick
}

func (m spinnerModel) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyMsg:
		if msg.String() == "ctrl+c" || msg.String() == "q" {
			m.quitting = true
			return m, tea.Quit
		}
	case spinner.TickMsg:
		var cmd tea.Cmd
		m.spinner, cmd = m.spinner.Update(msg)
		return m, cmd
	case commandDoneMsg:
		m.done = true
		m.success = msg.success
		return m, tea.Quit
	}
	return m, nil
}

func (m spinnerModel) View() string {
	if m.done {
		if m.success {
			return checkmarkStyle.Render("✓") + " " + m.message + "\n"
		}
		return crossStyle.Render("✗") + " " + m.message + "\n"
	}
	return m.spinner.View() + " " + m.message + "\n"
}

type commandDoneMsg struct {
	success bool
	output  string
	err     error
}

// ============================================================================
// Utility Functions
// ============================================================================

func clearScreen() {
	fmt.Print("\033[H\033[2J")
}

func displayBanner() {
	myFigure := figure.NewFigure("Agentle4j", "big", true)
	banner := myFigure.String()

	lines := strings.Split(banner, "\n")
	gradient := []lipgloss.Color{
		lipgloss.Color("#7C3AED"),
		lipgloss.Color("#8B5CF6"),
		lipgloss.Color("#A78BFA"),
		lipgloss.Color("#C4B5FD"),
		lipgloss.Color("#06B6D4"),
	}

	for i, line := range lines {
		colorIdx := i % len(gradient)
		style := lipgloss.NewStyle().Foreground(gradient[colorIdx]).Bold(true)
		fmt.Println(style.Render(line))
	}

	fmt.Println(subtitleStyle.Render("  Release Manager"))
	fmt.Println()
}

// askErrorAction prompts the user what to do when an error occurs
func askErrorAction(stepName string, errMsg string, canSkip bool, canRollback bool) ErrorAction {
	fmt.Println()
	fmt.Println(errorStyle.Render("✗ Error in: " + stepName))
	fmt.Println(mutedStyle.Render("  " + errMsg))
	fmt.Println()

	options := []huh.Option[string]{
		huh.NewOption("🔄 Retry this step", "retry"),
	}

	if canSkip {
		options = append(options, huh.NewOption("⏭️  Skip and continue", "skip"))
	}
	if canRollback {
		options = append(options, huh.NewOption("↩️  Rollback changes and abort", "rollback"))
	}
	options = append(options, huh.NewOption("🛑 Abort without rollback", "abort"))

	var choice string
	form := huh.NewForm(
		huh.NewGroup(
			huh.NewSelect[string]().
				Title("What would you like to do?").
				Options(options...).
				Value(&choice),
		),
	).WithTheme(getFormTheme())

	err := form.Run()
	if err != nil {
		return ActionAbort
	}

	switch choice {
	case "retry":
		return ActionRetry
	case "skip":
		return ActionSkip
	case "rollback":
		return ActionRollback
	default:
		return ActionAbort
	}
}

// runCommandWithResult runs a command and returns output and error
func runCommandWithResult(cmd *exec.Cmd) (string, error) {
	output, err := cmd.CombinedOutput()
	return string(output), err
}

func runCommandWithSpinner(description string, cmd *exec.Cmd) (string, error) {
	m := newSpinnerModel(description)

	var cmdOutput string
	var cmdErr error

	p := tea.NewProgram(m)

	go func() {
		output, err := cmd.CombinedOutput()
		cmdOutput = string(output)
		cmdErr = err
		success := err == nil

		// Handle specific cases
		if err != nil {
			// Git commit with nothing to commit is okay
			if strings.Contains(cmdOutput, "nothing to commit") {
				success = true
				cmdErr = nil
			}
		}

		p.Send(commandDoneMsg{
			success: success,
			output:  cmdOutput,
			err:     cmdErr,
		})
	}()

	finalModel, err := p.Run()
	if err != nil {
		return cmdOutput, err
	}

	if fm, ok := finalModel.(spinnerModel); ok {
		if fm.quitting {
			os.Exit(130)
		}
	}

	return cmdOutput, cmdErr
}

func getLatestRelease() (Version, error) {
	cmd := exec.Command("gh", "release", "list", "--json", "tagName", "--limit", "1")
	output, err := cmd.Output()
	if err != nil {
		return Version{}, err
	}

	var releases []GitHubRelease
	if err := json.Unmarshal(output, &releases); err != nil {
		return Version{}, err
	}

	if len(releases) == 0 {
		return Version{Major: 0, Minor: 0, Patch: 0}, nil
	}

	return ParseVersion(releases[0].TagName)
}

func releaseExists(version Version) bool {
	cmd := exec.Command("gh", "release", "view", version.String())
	err := cmd.Run()
	return err == nil
}

// WorkflowRun represents a GitHub Actions workflow run
type WorkflowRun struct {
	HeadBranch  string `json:"headBranch"`
	Status      string `json:"status"`
	Conclusion  string `json:"conclusion"`
	DisplayTitle string `json:"displayTitle"`
	CreatedAt   string `json:"createdAt"`
	URL         string `json:"url"`
}

// getLatestWorkflowForRelease checks if the latest publish workflow for a release succeeded
func getLatestWorkflowForRelease(version Version) (bool, string, error) {
	// Get workflow runs for the publish workflow
	cmd := exec.Command("gh", "run", "list", 
		"--workflow", "publish-to-maven-central.yml",
		"--json", "headBranch,status,conclusion,displayTitle,url",
		"--limit", "10",
	)
	output, err := cmd.Output()
	if err != nil {
		return false, "", err
	}

	var runs []WorkflowRun
	if err := json.Unmarshal(output, &runs); err != nil {
		return false, "", err
	}

	// Find the run for this version
	for _, run := range runs {
		if strings.Contains(run.HeadBranch, version.String()) || 
		   strings.Contains(run.DisplayTitle, version.String()) {
			if run.Conclusion == "success" {
				return true, run.URL, nil
			} else if run.Conclusion == "failure" {
				return false, run.URL, nil
			}
			// Still running
			return false, run.URL, fmt.Errorf("workflow still running")
		}
	}

	return false, "", fmt.Errorf("no workflow found for %s", version.String())
}

// getFailedReleases returns releases that exist in GitHub but may have failed workflows
func getAllReleases() ([]Version, error) {
	cmd := exec.Command("gh", "release", "list", "--json", "tagName", "--limit", "10")
	output, err := cmd.Output()
	if err != nil {
		return nil, err
	}

	var releases []GitHubRelease
	if err := json.Unmarshal(output, &releases); err != nil {
		return nil, err
	}

	var versions []Version
	for _, r := range releases {
		v, err := ParseVersion(r.TagName)
		if err == nil {
			versions = append(versions, v)
		}
	}
	return versions, nil
}

// retriggerWorkflow manually triggers the publish workflow for a tag
func retriggerWorkflow(version Version) error {
	// First, we need to re-run the failed workflow or trigger a new one
	// The simplest way is to use gh workflow run with the tag
	cmd := exec.Command("gh", "workflow", "run", "publish-to-maven-central.yml", "--ref", version.String())
	return cmd.Run()
}

// WorkflowStep represents a step in a GitHub Actions job
type WorkflowStep struct {
	Name       string `json:"name"`
	Conclusion string `json:"conclusion"`
	Status     string `json:"status"`
}

// WorkflowJob represents a job in a GitHub Actions workflow run
type WorkflowJob struct {
	Name       string         `json:"name"`
	Conclusion string         `json:"conclusion"`
	Status     string         `json:"status"`
	Steps      []WorkflowStep `json:"steps"`
}

// WorkflowDetails contains detailed information about a workflow run
type WorkflowDetails struct {
	RunID          int64
	URL            string
	Conclusion     string
	FailedSteps    []string
	SucceededSteps []string
	MavenPublished bool // True if Maven Central publish succeeded
}

// getWorkflowRunID gets the run ID for a version's workflow
func getWorkflowRunID(version Version) (int64, string, error) {
	cmd := exec.Command("gh", "run", "list",
		"--workflow", "publish-to-maven-central.yml",
		"--json", "headBranch,displayTitle,databaseId,url,conclusion",
		"--limit", "10",
	)
	output, err := cmd.Output()
	if err != nil {
		return 0, "", err
	}

	var runs []struct {
		HeadBranch   string `json:"headBranch"`
		DisplayTitle string `json:"displayTitle"`
		DatabaseId   int64  `json:"databaseId"`
		URL          string `json:"url"`
		Conclusion   string `json:"conclusion"`
	}
	if err := json.Unmarshal(output, &runs); err != nil {
		return 0, "", err
	}

	for _, run := range runs {
		if strings.Contains(run.HeadBranch, version.String()) ||
			strings.Contains(run.DisplayTitle, version.String()) {
			return run.DatabaseId, run.URL, nil
		}
	}
	return 0, "", fmt.Errorf("no workflow found for %s", version.String())
}

// getWorkflowDetails fetches detailed information about a workflow run
func getWorkflowDetails(version Version) (*WorkflowDetails, error) {
	runID, url, err := getWorkflowRunID(version)
	if err != nil {
		return nil, err
	}

	cmd := exec.Command("gh", "run", "view", fmt.Sprintf("%d", runID), "--json", "jobs,conclusion")
	output, err := cmd.Output()
	if err != nil {
		return nil, err
	}

	var result struct {
		Conclusion string        `json:"conclusion"`
		Jobs       []WorkflowJob `json:"jobs"`
	}
	if err := json.Unmarshal(output, &result); err != nil {
		return nil, err
	}

	details := &WorkflowDetails{
		RunID:      runID,
		URL:        url,
		Conclusion: result.Conclusion,
	}

	// Analyze steps
	for _, job := range result.Jobs {
		for _, step := range job.Steps {
			if step.Conclusion == "failure" {
				details.FailedSteps = append(details.FailedSteps, step.Name)
			} else if step.Conclusion == "success" {
				details.SucceededSteps = append(details.SucceededSteps, step.Name)
				// Check if Maven publish succeeded
				if strings.Contains(step.Name, "Publish to Maven") ||
					strings.Contains(step.Name, "deploy") {
					details.MavenPublished = true
				}
			}
		}
	}

	// Also check if "Build and verify" succeeded (means code compiled)
	for _, step := range details.SucceededSteps {
		if strings.Contains(step, "Build and verify") {
			// Build succeeded, likely Maven operations worked
		}
	}

	return details, nil
}

// formatWorkflowStatus returns a detailed status message for a workflow
func formatWorkflowStatus(details *WorkflowDetails) string {
	if details == nil {
		return "Could not fetch workflow details"
	}

	var sb strings.Builder

	if details.Conclusion == "success" {
		sb.WriteString("✅ All steps completed successfully")
		return sb.String()
	}

	if len(details.FailedSteps) > 0 {
		sb.WriteString("❌ Failed step(s): ")
		sb.WriteString(strings.Join(details.FailedSteps, ", "))
	}

	// Check if Maven published even though workflow failed
	mavenPublishSucceeded := false
	for _, step := range details.SucceededSteps {
		if strings.Contains(step, "Publish to Maven Central") {
			mavenPublishSucceeded = true
			break
		}
	}

	if mavenPublishSucceeded {
		sb.WriteString("\n   ✅ Maven Central publish SUCCEEDED!")
		sb.WriteString("\n   ℹ️  Your library IS published, the failure was in a later step.")
	}

	return sb.String()
}

// ============================================================================
// Changelog Management
// ============================================================================

const changelogFile = "CHANGELOG.md"

const changelogHeader = `# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

`

// ChangeType represents the type of change in a release
type ChangeType struct {
	Key         string
	Label       string
	Emoji       string
	Description string
}

var changeTypes = []ChangeType{
	{Key: "added", Label: "Added", Emoji: "✨", Description: "New features"},
	{Key: "changed", Label: "Changed", Emoji: "💥", Description: "Breaking changes or modifications"},
	{Key: "fixed", Label: "Fixed", Emoji: "🐛", Description: "Bug fixes"},
	{Key: "deprecated", Label: "Deprecated", Emoji: "⚠️", Description: "Features that will be removed"},
	{Key: "removed", Label: "Removed", Emoji: "🗑️", Description: "Removed features"},
	{Key: "security", Label: "Security", Emoji: "🔒", Description: "Security fixes"},
}

// ChangelogEntry represents a version's changelog entry
type ChangelogEntry struct {
	Version Version
	Date    string
	Changes map[string][]string // key is change type, value is list of changes
}

// changelogExists checks if CHANGELOG.md exists
func changelogExists() bool {
	_, err := os.Stat(changelogFile)
	return err == nil
}

// readChangelog reads the changelog file content
func readChangelog() (string, error) {
	content, err := os.ReadFile(changelogFile)
	if err != nil {
		if os.IsNotExist(err) {
			return "", nil
		}
		return "", err
	}
	return string(content), nil
}

// hasVersionInChangelog checks if a version is already documented
func hasVersionInChangelog(version Version) bool {
	content, err := readChangelog()
	if err != nil || content == "" {
		return false
	}

	// Look for ## [X.Y.Z] pattern
	pattern := fmt.Sprintf(`## \[%s\]`, regexp.QuoteMeta(version.PomString()))
	matched, _ := regexp.MatchString(pattern, content)
	return matched
}

// formatChangelogEntry formats an entry in Keep a Changelog format
func formatChangelogEntry(entry ChangelogEntry) string {
	var sb strings.Builder

	sb.WriteString(fmt.Sprintf("## [%s] - %s\n", entry.Version.PomString(), entry.Date))

	// Order: Added, Changed, Deprecated, Removed, Fixed, Security
	order := []string{"added", "changed", "deprecated", "removed", "fixed", "security"}

	for _, key := range order {
		if changes, ok := entry.Changes[key]; ok && len(changes) > 0 {
			// Find the label for this key
			label := strings.Title(key)
			for _, ct := range changeTypes {
				if ct.Key == key {
					label = ct.Label
					break
				}
			}

			sb.WriteString(fmt.Sprintf("### %s\n", label))
			for _, change := range changes {
				sb.WriteString(fmt.Sprintf("- %s\n", change))
			}
			sb.WriteString("\n")
		}
	}

	return sb.String()
}

// prependToChangelog adds a new entry at the top of the changelog
func prependToChangelog(entry ChangelogEntry) error {
	content, err := readChangelog()
	if err != nil {
		return err
	}

	formattedEntry := formatChangelogEntry(entry)

	var newContent string
	if content == "" || !strings.Contains(content, "# Changelog") {
		// Initialize changelog with header
		newContent = changelogHeader + formattedEntry
	} else {
		// Find where to insert (after the header section)
		// Look for the first "## [" which marks the start of version entries
		insertIdx := strings.Index(content, "## [")
		if insertIdx == -1 {
			// No versions yet, append after header
			newContent = content + "\n" + formattedEntry
		} else {
			// Insert before the first version
			newContent = content[:insertIdx] + formattedEntry + content[insertIdx:]
		}
	}

	return os.WriteFile(changelogFile, []byte(newContent), 0644)
}

// promptForChangelog shows an interactive prompt for entering changelog
func promptForChangelog(version Version) (*ChangelogEntry, error) {
	fmt.Println()
	fmt.Println(boxStyle.Render(titleStyle.Render("📝 Changelog Update Required")))
	fmt.Println()
	fmt.Println(infoStyle.Render("Version " + warningStyle.Render(version.String()) + " is not documented in CHANGELOG.md"))
	fmt.Println(mutedStyle.Render("Let's document what changed in this release."))
	fmt.Println()

	entry := &ChangelogEntry{
		Version: version,
		Date:    time.Now().Format("2006-01-02"),
		Changes: make(map[string][]string),
	}

	// Build options for change type multi-select
	var options []huh.Option[string]
	for _, ct := range changeTypes {
		options = append(options, huh.NewOption(
			fmt.Sprintf("%s %s (%s)", ct.Emoji, ct.Label, ct.Description),
			ct.Key,
		))
	}

	var selectedTypes []string
	selectForm := huh.NewForm(
		huh.NewGroup(
			huh.NewMultiSelect[string]().
				Title("What types of changes are in this release?").
				Description("Space to select, Enter to confirm").
				Options(options...).
				Value(&selectedTypes),
		),
	).WithTheme(getFormTheme())

	if err := selectForm.Run(); err != nil {
		return nil, err
	}

	if len(selectedTypes) == 0 {
		fmt.Println(warningStyle.Render("⚠ No change types selected. Skipping changelog."))
		return nil, nil
	}

	// For each selected type, prompt for changes
	for _, typeKey := range selectedTypes {
		var typeInfo ChangeType
		for _, ct := range changeTypes {
			if ct.Key == typeKey {
				typeInfo = ct
				break
			}
		}

		fmt.Println()
		fmt.Println(stepStyle.Render(typeInfo.Emoji+" "+typeInfo.Label+":"))

		var changesText string
		inputForm := huh.NewForm(
			huh.NewGroup(
				huh.NewText().
					Title("Enter changes (one per line)").
					Description("Describe what was " + strings.ToLower(typeInfo.Label)).
					Placeholder("- Feature 1\n- Feature 2").
					CharLimit(2000).
					Value(&changesText),
			),
		).WithTheme(getFormTheme())

		if err := inputForm.Run(); err != nil {
			return nil, err
		}

		// Parse the text into individual changes
		if changesText != "" {
			lines := strings.Split(changesText, "\n")
			for _, line := range lines {
				line = strings.TrimSpace(line)
				line = strings.TrimPrefix(line, "-")
				line = strings.TrimPrefix(line, "*")
				line = strings.TrimSpace(line)
				if line != "" {
					entry.Changes[typeKey] = append(entry.Changes[typeKey], line)
				}
			}
		}
	}

	// Check if any changes were actually entered
	hasChanges := false
	for _, changes := range entry.Changes {
		if len(changes) > 0 {
			hasChanges = true
			break
		}
	}

	if !hasChanges {
		fmt.Println(warningStyle.Render("⚠ No changes entered. Skipping changelog."))
		return nil, nil
	}

	// Show preview
	fmt.Println()
	previewBox := lipgloss.NewStyle().
		Border(lipgloss.RoundedBorder()).
		BorderForeground(secondaryColor).
		Padding(1, 2).
		MarginTop(1)

	preview := formatChangelogEntry(*entry)
	fmt.Println(previewBox.Render(
		infoStyle.Render("📋 Changelog Preview") + "\n\n" + preview,
	))

	// Confirm
	var confirmed bool
	confirmForm := huh.NewForm(
		huh.NewGroup(
			huh.NewConfirm().
				Title("Add this to CHANGELOG.md?").
				Affirmative("Yes, add it").
				Negative("No, skip").
				Value(&confirmed),
		),
	).WithTheme(getFormTheme())

	if err := confirmForm.Run(); err != nil {
		return nil, err
	}

	if !confirmed {
		fmt.Println(warningStyle.Render("⚠ Changelog update skipped."))
		return nil, nil
	}

	return entry, nil
}

// stepUpdateChangelog ensures changelog is updated for the new version
func stepUpdateChangelog(state *ReleaseState) bool {
	fmt.Println(stepStyle.Render("Step 1/6: ") + "Checking changelog")

	// Check if version is already documented
	if hasVersionInChangelog(state.NewVersion) {
		fmt.Println(checkmarkStyle.Render("✓") + " Changelog already has entry for " + state.NewVersion.PomString())
		return true
	}

	// Prompt for changelog entry
	entry, err := promptForChangelog(state.NewVersion)
	if err != nil {
		fmt.Println(errorStyle.Render("✗ Error during changelog prompt: " + err.Error()))
		return false
	}

	if entry == nil {
		// User chose to skip - ask if they want to continue without changelog
		var continueWithout bool
		form := huh.NewForm(
			huh.NewGroup(
				huh.NewConfirm().
					Title("Continue release without changelog entry?").
					Description("This is not recommended for public releases").
					Affirmative("Yes, continue").
					Negative("No, abort").
					Value(&continueWithout),
			),
		).WithTheme(getFormTheme())

		if err := form.Run(); err != nil || !continueWithout {
			fmt.Println(warningStyle.Render("Release aborted."))
			return false
		}
		fmt.Println(warningStyle.Render("⚠") + " Continuing without changelog entry")
		return true
	}

	// Write changelog entry
	if err := prependToChangelog(*entry); err != nil {
		fmt.Println(errorStyle.Render("✗ Could not update CHANGELOG.md: " + err.Error()))
		return false
	}

	fmt.Println(checkmarkStyle.Render("✓") + " Updated CHANGELOG.md with " + state.NewVersion.PomString() + " entry")
	return true
}

func getPomVersion() (Version, error) {
	content, err := os.ReadFile("pom.xml")
	if err != nil {
		return Version{}, err
	}

	re := regexp.MustCompile(`<version>([0-9]+\.[0-9]+\.[0-9]+)</version>`)
	matches := re.FindSubmatch(content)
	if len(matches) < 2 {
		return Version{}, fmt.Errorf("could not find version in pom.xml")
	}

	return ParseVersion(string(matches[1]))
}

func getPomContent() ([]byte, error) {
	return os.ReadFile("pom.xml")
}

func updatePomVersion(newVersion Version) error {
	content, err := os.ReadFile("pom.xml")
	if err != nil {
		return err
	}

	re := regexp.MustCompile(`(<version>)([0-9]+\.[0-9]+\.[0-9]+)(</version>)`)
	updated := replaceFirst(re, content, []byte("${1}"+newVersion.PomString()+"${3}"))

	return os.WriteFile("pom.xml", updated, 0644)
}

func restorePom(content []byte) error {
	return os.WriteFile("pom.xml", content, 0644)
}

// replaceFirst replaces only the first occurrence of the regex match
func replaceFirst(re *regexp.Regexp, src, repl []byte) []byte {
	loc := re.FindIndex(src)
	if loc == nil {
		return src
	}

	match := src[loc[0]:loc[1]]
	replacement := re.Expand(nil, repl, match, re.FindSubmatchIndex(match))

	result := make([]byte, 0, len(src)-len(match)+len(replacement))
	result = append(result, src[:loc[0]]...)
	result = append(result, replacement...)
	result = append(result, src[loc[1]:]...)
	return result
}

func checkGitHubCLI() bool {
	_, err := exec.LookPath("gh")
	return err == nil
}

func checkGit() bool {
	_, err := exec.LookPath("git")
	return err == nil
}

// rollback attempts to restore the original state
func rollback(state *ReleaseState) {
	fmt.Println()
	fmt.Println(boxStyle.Render(warningStyle.Render("↩️  Rolling back changes...")))

	if state.ChangesCommitted && !state.ChangesPushed {
		// Reset the last commit
		cmd := exec.Command("git", "reset", "--soft", "HEAD~1")
		if err := cmd.Run(); err != nil {
			fmt.Println(errorStyle.Render("  ✗ Could not reset commit"))
		} else {
			fmt.Println(checkmarkStyle.Render("  ✓ Reset last commit"))
		}
	}

	if state.ChangesStaged {
		// Unstage changes
		cmd := exec.Command("git", "reset", "HEAD")
		if err := cmd.Run(); err != nil {
			fmt.Println(errorStyle.Render("  ✗ Could not unstage changes"))
		} else {
			fmt.Println(checkmarkStyle.Render("  ✓ Unstaged changes"))
		}
	}

	if state.PomModified && len(state.OriginalPomContent) > 0 {
		// Restore original pom.xml
		if err := restorePom(state.OriginalPomContent); err != nil {
			fmt.Println(errorStyle.Render("  ✗ Could not restore pom.xml"))
		} else {
			fmt.Println(checkmarkStyle.Render("  ✓ Restored pom.xml to original version"))
		}
	}

	fmt.Println()
	fmt.Println(infoStyle.Render("Rollback complete. You can safely try again."))
}

// ============================================================================
// Release Steps
// ============================================================================

func stepUpdatePom(state *ReleaseState) bool {
	fmt.Println(stepStyle.Render("Step 2/6: ") + "Updating pom.xml version")

	for {
		err := updatePomVersion(state.NewVersion)
		if err == nil {
			state.PomModified = true
			fmt.Println(checkmarkStyle.Render("✓") + " Updated pom.xml to " + state.NewVersion.PomString())
			return true
		}

		action := askErrorAction("Update pom.xml", err.Error(), false, false)
		switch action {
		case ActionRetry:
			continue
		case ActionAbort:
			return false
		default:
			return false
		}
	}
}

func stepStageChanges(state *ReleaseState) bool {
	fmt.Println()
	fmt.Println(stepStyle.Render("Step 3/6: ") + "Staging changes")

	for {
		cmd := exec.Command("git", "add", ".")
		output, err := runCommandWithSpinner("Staging all changes", cmd)
		if err == nil {
			state.ChangesStaged = true
			return true
		}

		action := askErrorAction("Stage changes", output, false, true)
		switch action {
		case ActionRetry:
			continue
		case ActionRollback:
			rollback(state)
			return false
		case ActionAbort:
			return false
		default:
			return false
		}
	}
}

func stepCommit(state *ReleaseState) bool {
	fmt.Println()
	fmt.Println(stepStyle.Render("Step 4/6: ") + "Creating commit")

	for {
		cmd := exec.Command("git", "commit", "-m", fmt.Sprintf("Release %s", state.NewVersion.String()))
		output, err := runCommandWithSpinner("Committing changes", cmd)

		// Check for "nothing to commit" which is acceptable
		if err != nil && strings.Contains(output, "nothing to commit") {
			fmt.Println(infoStyle.Render("ℹ Nothing new to commit (this is okay)"))
			return true
		}

		if err == nil {
			state.ChangesCommitted = true
			return true
		}

		action := askErrorAction("Create commit", output, true, true)
		switch action {
		case ActionRetry:
			continue
		case ActionSkip:
			fmt.Println(warningStyle.Render("  ⚠ Skipping commit step"))
			return true
		case ActionRollback:
			rollback(state)
			return false
		case ActionAbort:
			return false
		default:
			return false
		}
	}
}

func stepPush(state *ReleaseState) bool {
	fmt.Println()
	fmt.Println(stepStyle.Render("Step 5/6: ") + "Pushing to remote")

	for {
		cmd := exec.Command("git", "push")
		output, err := runCommandWithSpinner("Pushing to GitHub", cmd)
		if err == nil {
			state.ChangesPushed = true
			return true
		}

		action := askErrorAction("Push to GitHub", output, false, true)
		switch action {
		case ActionRetry:
			continue
		case ActionRollback:
			rollback(state)
			return false
		case ActionAbort:
			fmt.Println()
			fmt.Println(warningStyle.Render("⚠ Changes are committed locally but not pushed."))
			fmt.Println(mutedStyle.Render("  To push manually: git push"))
			fmt.Println(mutedStyle.Render("  To undo commit: git reset --soft HEAD~1"))
			return false
		default:
			return false
		}
	}
}

func stepCreateRelease(state *ReleaseState) bool {
	fmt.Println()
	fmt.Println(stepStyle.Render("Step 6/6: ") + "Creating GitHub release")

	// Get release title
	var releaseTitle string
	titleForm := huh.NewForm(
		huh.NewGroup(
			huh.NewInput().
				Title("Release title").
				Description("Leave empty to use version as title").
				Placeholder(state.NewVersion.String()).
				Value(&releaseTitle),
		),
	).WithTheme(getFormTheme())

	err := titleForm.Run()
	if err != nil {
		releaseTitle = state.NewVersion.String()
	}
	if releaseTitle == "" {
		releaseTitle = state.NewVersion.String()
	}

	for {
		cmd := exec.Command("gh", "release", "create",
			state.NewVersion.String(),
			"--title", releaseTitle,
			"--generate-notes",
		)
		output, err := runCommandWithSpinner("Creating GitHub release", cmd)
		if err == nil {
			state.ReleaseCreated = true
			return true
		}

		action := askErrorAction("Create GitHub release", output, true, false)
		switch action {
		case ActionRetry:
			continue
		case ActionSkip:
			fmt.Println()
			fmt.Println(warningStyle.Render("⚠ Release not created. You can create it manually:"))
			fmt.Println(mutedStyle.Render("  gh release create " + state.NewVersion.String() + " --title \"" + releaseTitle + "\" --generate-notes"))
			fmt.Println(mutedStyle.Render("  Or via GitHub UI: https://github.com/paragon-intelligence/agentle4j/releases/new"))
			return true // Continue to success (push was done)
		case ActionAbort:
			fmt.Println()
			fmt.Println(warningStyle.Render("⚠ Code is pushed but release not created."))
			fmt.Println(mutedStyle.Render("  Create release manually: gh release create " + state.NewVersion.String()))
			return false
		default:
			return false
		}
	}
}

// ============================================================================
// Republish & Status Check Handlers
// ============================================================================

func handleRepublish() {
	fmt.Println()
	fmt.Println(boxStyle.Render(titleStyle.Render("🔄 Republish Existing Release")))
	fmt.Println()

	// Get all releases
	releases, err := getAllReleases()
	if err != nil || len(releases) == 0 {
		fmt.Println(errorStyle.Render("✗ No releases found to republish"))
		fmt.Println(mutedStyle.Render("  Create a new release first with 'make release'"))
		os.Exit(1)
	}

	// Build options with workflow status
	var options []huh.Option[string]
	for _, v := range releases {
		success, url, err := getLatestWorkflowForRelease(v)
		var status string
		if err != nil {
			if strings.Contains(err.Error(), "still running") {
				status = "⏳ Running"
			} else {
				status = "❓ Unknown"
			}
		} else if success {
			status = "✅ Success"
		} else {
			status = "❌ Failed"
		}
		_ = url // We'll show URL later if needed
		options = append(options, huh.NewOption(
			fmt.Sprintf("%s - %s", v.String(), status),
			v.String(),
		))
	}

	var selectedVersion string
	form := huh.NewForm(
		huh.NewGroup(
			huh.NewSelect[string]().
				Title("Select release to republish:").
				Description("Releases with ❌ Failed status can be republished").
				Options(options...).
				Value(&selectedVersion),
		),
	).WithTheme(getFormTheme())

	err = form.Run()
	if err != nil {
		fmt.Println(warningStyle.Render("Cancelled."))
		os.Exit(130)
	}

	version, _ := ParseVersion(selectedVersion)

	// Check current workflow status
	success, url, _ := getLatestWorkflowForRelease(version)
	if success {
		fmt.Println()
		fmt.Println(successStyle.Render("✓ This release already has a successful workflow!"))
		fmt.Println(mutedStyle.Render("  The artifact should already be on Maven Central."))
		fmt.Println(mutedStyle.Render("  Check: https://central.sonatype.com/search?q=agentle4j"))
		return
	}

	// Check if Maven Central publish succeeded even though workflow failed
	details, detailsErr := getWorkflowDetails(version)
	if detailsErr == nil && details != nil {
		// Check if Maven publish step succeeded
		mavenPublishSucceeded := false
		for _, step := range details.SucceededSteps {
			if strings.Contains(step, "Publish to Maven Central") {
				mavenPublishSucceeded = true
				break
			}
		}

		if mavenPublishSucceeded {
			fmt.Println()
			fmt.Println(boxStyle.Copy().BorderForeground(errorColor).Render(
				errorStyle.Render("🚫 Cannot Republish to Maven Central") + "\n\n" +
					"  Version " + warningStyle.Render(version.String()) + " was already published to Maven Central.\n\n" +
					"  " + mutedStyle.Render("Maven Central does not allow overwriting existing versions.") + "\n" +
					"  " + mutedStyle.Render("The workflow failed in a LATER step (e.g., GitHub Release).") + "\n\n" +
					"  " + infoStyle.Render("Options:") + "\n" +
					"  • Create a new patch version (e.g., " + version.Bump(Patch).String() + ")\n" +
					"  • Fix the failed step manually if needed",
			))
			fmt.Println()
			fmt.Println(mutedStyle.Render("  Check Maven Central: https://central.sonatype.com/search?q=agentle4j"))
			return
		}
	}

	// Confirm republish
	fmt.Println()
	fmt.Println(warningStyle.Render("⚠ This will trigger a new workflow run for " + version.String()))
	if url != "" {
		fmt.Println(mutedStyle.Render("  Previous run: " + url))
	}
	fmt.Println()

	var confirmed bool
	confirmForm := huh.NewForm(
		huh.NewGroup(
			huh.NewConfirm().
				Title("Trigger new workflow for " + version.String() + "?").
				Description("This will attempt to publish to Maven Central again").
				Affirmative("Yes, republish").
				Negative("Cancel").
				Value(&confirmed),
		),
	).WithTheme(getFormTheme())

	err = confirmForm.Run()
	if err != nil || !confirmed {
		fmt.Println(warningStyle.Render("Cancelled."))
		return
	}

	// Trigger the workflow
	fmt.Println()
	fmt.Println(stepStyle.Render("Triggering workflow..."))

	err = retriggerWorkflow(version)
	if err != nil {
		fmt.Println(errorStyle.Render("✗ Could not trigger workflow automatically"))
		fmt.Println()
		fmt.Println(infoStyle.Render("You can manually trigger it:"))
		fmt.Println(mutedStyle.Render("  1. Go to: https://github.com/paragon-intelligence/agentle4j/actions"))
		fmt.Println(mutedStyle.Render("  2. Click 'Publish to Maven Central' workflow"))
		fmt.Println(mutedStyle.Render("  3. Click 'Run workflow' dropdown"))
		fmt.Println(mutedStyle.Render("  4. Select tag: " + version.String()))
		fmt.Println(mutedStyle.Render("  5. Click 'Run workflow'"))
		return
	}

	fmt.Println(successStyle.Render("✓ Workflow triggered successfully!"))
	fmt.Println()
	fmt.Println(infoStyle.Render("Monitor progress at:"))
	fmt.Println(mutedStyle.Render("  https://github.com/paragon-intelligence/agentle4j/actions"))
}

func handleStatusCheck() {
	fmt.Println()
	fmt.Println(boxStyle.Render(titleStyle.Render("📊 Workflow Status Check")))
	fmt.Println()

	// Get all releases
	releases, err := getAllReleases()
	if err != nil || len(releases) == 0 {
		fmt.Println(infoStyle.Render("ℹ No releases found"))
		return
	}

	fmt.Println(mutedStyle.Render("Checking workflow status for recent releases...\n"))

	for _, v := range releases {
		success, url, err := getLatestWorkflowForRelease(v)
		
		var statusIcon string
		var statusText string
		
		if err != nil {
			if strings.Contains(err.Error(), "still running") {
				statusIcon = "⏳"
				statusText = "Running"
			} else {
				statusIcon = "❓"
				statusText = "No workflow found"
			}
		} else if success {
			statusIcon = "✅"
			statusText = "Published to Maven Central"
		} else {
			statusIcon = "❌"
			statusText = "FAILED - needs republish!"
		}

		fmt.Printf("  %s %s - %s\n", statusIcon, v.String(), statusText)
		if url != "" && !success {
			fmt.Println(mutedStyle.Render("     " + url))
		}
	}

	fmt.Println()
	fmt.Println(infoStyle.Render("Use 'Republish existing release' to retry failed workflows."))
}

// deleteTagAndRelease deletes both the GitHub release and the git tag (local and remote)
func deleteTagAndRelease(version Version) error {
	// Delete GitHub release first
	cmd := exec.Command("gh", "release", "delete", version.String(), "--yes")
	if output, err := cmd.CombinedOutput(); err != nil {
		return fmt.Errorf("could not delete release: %s", string(output))
	}

	// Delete remote tag
	cmd = exec.Command("git", "push", "--delete", "origin", version.String())
	if output, err := cmd.CombinedOutput(); err != nil {
		return fmt.Errorf("could not delete remote tag: %s", string(output))
	}

	// Delete local tag
	cmd = exec.Command("git", "tag", "-d", version.String())
	cmd.Run() // Ignore error if local tag doesn't exist

	return nil
}

func handleRecreateRelease() {
	fmt.Println()
	fmt.Println(boxStyle.Render(titleStyle.Render("🔄 Recreate Release (Delete & Republish)")))
	fmt.Println()
	fmt.Println(warningStyle.Render("⚠️  This will:"))
	fmt.Println(mutedStyle.Render("   1. Delete the existing GitHub release and tag"))
	fmt.Println(mutedStyle.Render("   2. Create a new tag from the CURRENT code"))
	fmt.Println(mutedStyle.Render("   3. Create a new release and trigger the publish workflow"))
	fmt.Println()
	fmt.Println(infoStyle.Render("Use this when your release was created from old/broken code."))
	fmt.Println()

	// Get all releases
	releases, err := getAllReleases()
	if err != nil || len(releases) == 0 {
		fmt.Println(errorStyle.Render("✗ No releases found to recreate"))
		return
	}

	// Build options with workflow status
	var options []huh.Option[string]
	for _, v := range releases {
		success, _, err := getLatestWorkflowForRelease(v)
		var status string
		if err != nil {
			if strings.Contains(err.Error(), "still running") {
				status = "⏳ Running"
			} else {
				status = "❓ Unknown"
			}
		} else if success {
			status = "✅ Success"
		} else {
			status = "❌ Failed"
		}
		options = append(options, huh.NewOption(
			fmt.Sprintf("%s - %s", v.String(), status),
			v.String(),
		))
	}

	var selectedVersion string
	form := huh.NewForm(
		huh.NewGroup(
			huh.NewSelect[string]().
				Title("Select release to recreate:").
				Description("⚠️ The release will be deleted and recreated from current code").
				Options(options...).
				Value(&selectedVersion),
		),
	).WithTheme(getFormTheme())

	err = form.Run()
	if err != nil {
		fmt.Println(warningStyle.Render("Cancelled."))
		return
	}

	version, _ := ParseVersion(selectedVersion)

	// Double confirmation
	fmt.Println()
	fmt.Println(boxStyle.Copy().BorderForeground(errorColor).Render(
		errorStyle.Render("⚠️  DANGER ZONE") + "\n\n" +
			"  This will PERMANENTLY DELETE " + warningStyle.Render(version.String()) + " and recreate it.\n" +
			"  " + mutedStyle.Render("The new release will use the current code on your branch."),
	))
	fmt.Println()

	var confirmed bool
	confirmForm := huh.NewForm(
		huh.NewGroup(
			huh.NewConfirm().
				Title("Are you absolutely sure?").
				Description("Type 'Yes' to delete " + version.String() + " and recreate it").
				Affirmative("Yes, delete and recreate").
				Negative("Cancel").
				Value(&confirmed),
		),
	).WithTheme(getFormTheme())

	err = confirmForm.Run()
	if err != nil || !confirmed {
		fmt.Println(warningStyle.Render("Cancelled."))
		return
	}

	// Step 1: Delete the release and tag
	fmt.Println()
	fmt.Println(stepStyle.Render("Step 1/3: ") + "Deleting release and tag...")

	deleteCmd := exec.Command("gh", "release", "delete", version.String(), "--yes")
	output, err := runCommandWithSpinner("Deleting GitHub release", deleteCmd)
	if err != nil && !strings.Contains(output, "not found") {
		fmt.Println(errorStyle.Render("✗ Could not delete release: " + output))
		return
	}

	deleteTagRemoteCmd := exec.Command("git", "push", "--delete", "origin", version.String())
	output, err = runCommandWithSpinner("Deleting remote tag", deleteTagRemoteCmd)
	if err != nil && !strings.Contains(output, "not found") && !strings.Contains(output, "remote ref does not exist") {
		fmt.Println(errorStyle.Render("✗ Could not delete remote tag: " + output))
		return
	}

	deleteTagLocalCmd := exec.Command("git", "tag", "-d", version.String())
	deleteTagLocalCmd.Run() // Ignore error

	fmt.Println(checkmarkStyle.Render("✓") + " Deleted release and tag")

	// Step 2: Create new tag
	fmt.Println()
	fmt.Println(stepStyle.Render("Step 2/3: ") + "Creating new tag from current code...")

	createTagCmd := exec.Command("git", "tag", version.String())
	output, err = runCommandWithSpinner("Creating local tag", createTagCmd)
	if err != nil {
		fmt.Println(errorStyle.Render("✗ Could not create tag: " + output))
		return
	}

	pushTagCmd := exec.Command("git", "push", "origin", version.String())
	output, err = runCommandWithSpinner("Pushing tag to GitHub", pushTagCmd)
	if err != nil {
		fmt.Println(errorStyle.Render("✗ Could not push tag: " + output))
		return
	}

	fmt.Println(checkmarkStyle.Render("✓") + " Created new tag from current code")

	// Step 3: Create new release
	fmt.Println()
	fmt.Println(stepStyle.Render("Step 3/3: ") + "Creating new GitHub release...")

	createReleaseCmd := exec.Command("gh", "release", "create",
		version.String(),
		"--title", version.String(),
		"--generate-notes",
	)
	output, err = runCommandWithSpinner("Creating GitHub release", createReleaseCmd)
	if err != nil {
		fmt.Println(errorStyle.Render("✗ Could not create release: " + output))
		fmt.Println()
		fmt.Println(infoStyle.Render("The tag was pushed. You can create the release manually:"))
		fmt.Println(mutedStyle.Render("  gh release create " + version.String() + " --generate-notes"))
		return
	}

	fmt.Println(checkmarkStyle.Render("✓") + " Created new release")

	// Success!
	fmt.Println()
	fmt.Println(boxStyle.Copy().BorderForeground(successColor).Render(
		successStyle.Render("🎉 Release Recreated Successfully!") + "\n\n" +
			"  " + infoStyle.Render(version.String()) + " has been recreated from the current code.\n" +
			"  The publish workflow should start automatically.\n\n" +
			"  " + mutedStyle.Render("Monitor: https://github.com/paragon-intelligence/agentle4j/actions"),
	))
}

// ============================================================================
// Main Flow
// ============================================================================

func main() {
	// Change to project root directory (two levels up from tools/releaser)
	if err := os.Chdir("../.."); err != nil {
		fmt.Println("Error: Could not change to project root directory")
		os.Exit(1)
	}

	clearScreen()
	displayBanner()

	// Initialize release state
	state := &ReleaseState{}

	// Check prerequisites
	fmt.Println(boxStyle.Render(titleStyle.Render("📋 Checking Prerequisites")))

	if !checkGit() {
		fmt.Println(errorStyle.Render("✗ Git is not installed"))
		os.Exit(1)
	}
	fmt.Println(checkmarkStyle.Render("✓") + " Git found")

	if !checkGitHubCLI() {
		fmt.Println(errorStyle.Render("✗ GitHub CLI (gh) is not installed"))
		os.Exit(1)
	}
	fmt.Println(checkmarkStyle.Render("✓") + " GitHub CLI found")

	// Backup original pom.xml content
	originalPom, err := getPomContent()
	if err != nil {
		fmt.Println(errorStyle.Render("✗ Could not read pom.xml: " + err.Error()))
		os.Exit(1)
	}
	state.OriginalPomContent = originalPom

	// Get current versions
	pomVersion, err := getPomVersion()
	if err != nil {
		fmt.Println(errorStyle.Render("✗ Could not parse pom.xml version: " + err.Error()))
		os.Exit(1)
	}
	fmt.Println(checkmarkStyle.Render("✓") + " pom.xml version: " + infoStyle.Render(pomVersion.PomString()))

	latestRelease, err := getLatestRelease()
	if err != nil {
		fmt.Println(warningStyle.Render("⚠ Could not fetch GitHub releases (this is okay for first release)"))
		latestRelease = Version{Major: 0, Minor: 0, Patch: 0}
	} else if latestRelease.IsZero() {
		fmt.Println(infoStyle.Render("ℹ No existing releases found (first release!)"))
	} else {
		fmt.Println(checkmarkStyle.Render("✓") + " Latest release: " + infoStyle.Render(latestRelease.String()))
	}

	fmt.Println()

	// Determine if this is a first release
	isFirstRelease := latestRelease.IsZero()

	// Only show main menu if there are existing releases
	// For first release, go straight to the release flow
	if !isFirstRelease {
		// Check if the latest release workflow failed
		workflowSuccess, workflowURL, workflowErr := getLatestWorkflowForRelease(latestRelease)
		
		if workflowErr == nil && !workflowSuccess {
			// Latest workflow FAILED - get detailed information
			details, detailsErr := getWorkflowDetails(latestRelease)
			
			// Build the warning message
			warningMsg := errorStyle.Render("⚠️  Warning: Previous Release Workflow Failed!") + "\n\n" +
				"  The workflow for " + warningStyle.Render(latestRelease.String()) + " did not fully complete."
			
			if detailsErr == nil && details != nil {
				warningMsg += "\n\n  " + formatWorkflowStatus(details)
			} else {
				warningMsg += "\n  " + mutedStyle.Render("Could not determine which step failed.")
			}
			
			fmt.Println(boxStyle.Copy().BorderForeground(errorColor).Render(warningMsg))
			
			if workflowURL != "" {
				fmt.Println(mutedStyle.Render("  Workflow run: " + workflowURL))
			}
			fmt.Println()

			var failedAction string
			failedForm := huh.NewForm(
				huh.NewGroup(
					huh.NewSelect[string]().
						Title("What would you like to do?").
						Options(
							huh.NewOption("🔄 Republish "+latestRelease.String()+" (retry with same code)", "republish"),
							huh.NewOption("🗑️  Recreate "+latestRelease.String()+" (delete & rebuild from CURRENT code)", "recreate"),
							huh.NewOption("🚀 Skip and create a NEW release (leave gap in versions)", "new"),
							huh.NewOption("📊 Check all workflow statuses", "status"),
						).
						Value(&failedAction),
				),
			).WithTheme(getFormTheme())

			err = failedForm.Run()
			if err != nil {
				fmt.Println(warningStyle.Render("Cancelled."))
				os.Exit(130)
			}

			if failedAction == "recreate" {
				handleRecreateRelease()
				return
			}

			if failedAction == "republish" {
				// Directly republish the failed release
				fmt.Println()
				fmt.Println(stepStyle.Render("Triggering workflow for " + latestRelease.String() + "..."))

				err = retriggerWorkflow(latestRelease)
				if err != nil {
					fmt.Println(errorStyle.Render("✗ Could not trigger workflow automatically"))
					fmt.Println()
					fmt.Println(infoStyle.Render("You can manually trigger it:"))
					fmt.Println(mutedStyle.Render("  1. Go to: https://github.com/paragon-intelligence/agentle4j/actions"))
					fmt.Println(mutedStyle.Render("  2. Click 'Publish to Maven Central' workflow"))
					fmt.Println(mutedStyle.Render("  3. Click 'Run workflow' dropdown"))
					fmt.Println(mutedStyle.Render("  4. Select tag: " + latestRelease.String()))
					fmt.Println(mutedStyle.Render("  5. Click 'Run workflow'"))
				} else {
					fmt.Println(successStyle.Render("✓ Workflow triggered successfully!"))
					fmt.Println()
					fmt.Println(infoStyle.Render("Monitor progress at:"))
					fmt.Println(mutedStyle.Render("  https://github.com/paragon-intelligence/agentle4j/actions"))
				}
				return
			}

			if failedAction == "status" {
				handleStatusCheck()
				return
			}
			// Continue with new release if "new" was chosen
		} else if workflowErr != nil && strings.Contains(workflowErr.Error(), "still running") {
			// Workflow is currently running
			fmt.Println(boxStyle.Copy().BorderForeground(warningColor).Render(
				warningStyle.Render("⏳ Workflow In Progress") + "\n\n" +
				"  The publish workflow for " + infoStyle.Render(latestRelease.String()) + " is still running.\n" +
				"  " + mutedStyle.Render("Wait for it to complete before creating a new release."),
			))
			if workflowURL != "" {
				fmt.Println(mutedStyle.Render("  Watch progress: " + workflowURL))
			}
			fmt.Println()

			var waitAction string
			waitForm := huh.NewForm(
				huh.NewGroup(
					huh.NewSelect[string]().
						Title("What would you like to do?").
						Options(
							huh.NewOption("🚀 Create new release anyway", "new"),
							huh.NewOption("❌ Exit and wait for workflow", "exit"),
						).
						Value(&waitAction),
				),
			).WithTheme(getFormTheme())

			err = waitForm.Run()
			if err != nil || waitAction == "exit" {
				fmt.Println(infoStyle.Render("\nExiting. Run 'make release' again after workflow completes."))
				return
			}
		} else {
			// No issues detected - show normal menu
			var mainAction string
			mainMenuOptions := []huh.Option[string]{
				huh.NewOption("🚀 Create new release", "new"),
				huh.NewOption("🔄 Republish existing release", "republish"),
				huh.NewOption("�️  Recreate release (delete & rebuild)", "recreate"),
				huh.NewOption("�📊 Check workflow status", "status"),
			}

			mainForm := huh.NewForm(
				huh.NewGroup(
					huh.NewSelect[string]().
						Title("What would you like to do?").
						Options(mainMenuOptions...).
						Value(&mainAction),
				),
			).WithTheme(getFormTheme())

			err = mainForm.Run()
			if err != nil {
				fmt.Println(warningStyle.Render("Cancelled."))
				os.Exit(130)
			}

			if mainAction == "recreate" {
				handleRecreateRelease()
				return
			}

			if mainAction == "republish" {
				handleRepublish()
				return
			}

			if mainAction == "status" {
				handleStatusCheck()
				return
			}
		}
	}
	var newVersion Version

	if isFirstRelease {
		// First release - offer to use current pom.xml version or customize
		fmt.Println(boxStyle.Render(titleStyle.Render("🎉 First Release Detected!")))
		fmt.Println(infoStyle.Render("No existing releases found. Your pom.xml version is: ") + successStyle.Render(pomVersion.PomString()))
		fmt.Println()

		var versionChoice string
		form := huh.NewForm(
			huh.NewGroup(
				huh.NewSelect[string]().
					Title("What version do you want to release?").
					Options(
						huh.NewOption(fmt.Sprintf("📦 Use current version (%s)", pomVersion.PomString()), "current"),
						huh.NewOption("✏️  Enter a custom version", "custom"),
					).
					Value(&versionChoice),
			),
		).WithTheme(getFormTheme())

		err = form.Run()
		if err != nil {
			fmt.Println(warningStyle.Render("Cancelled."))
			os.Exit(130)
		}

		if versionChoice == "custom" {
			var customVersion string
			customForm := huh.NewForm(
				huh.NewGroup(
					huh.NewInput().
						Title("Enter version").
						Description("Format: X.Y.Z (e.g., 0.1.0, 1.0.0)").
						Placeholder("0.1.0").
						Value(&customVersion).
						Validate(func(s string) error {
							_, err := ParseVersion(s)
							return err
						}),
				),
			).WithTheme(getFormTheme())

			err = customForm.Run()
			if err != nil {
				fmt.Println(warningStyle.Render("Cancelled."))
				os.Exit(130)
			}

			newVersion, _ = ParseVersion(customVersion)
		} else {
			newVersion = pomVersion
		}
	} else {
		// Not first release - offer bump options
		var releaseType string
		form := huh.NewForm(
			huh.NewGroup(
				huh.NewSelect[string]().
					Title("What kind of release are you doing?").
					Description(fmt.Sprintf("Current: %s", latestRelease.String())).
					Options(
						huh.NewOption(fmt.Sprintf("🐛 Patch  (%s → %s)", latestRelease.String(), latestRelease.Bump(Patch).String()), "patch"),
						huh.NewOption(fmt.Sprintf("✨ Feature (%s → %s)", latestRelease.String(), latestRelease.Bump(Feature).String()), "feature"),
						huh.NewOption(fmt.Sprintf("🚀 Major  (%s → %s)", latestRelease.String(), latestRelease.Bump(Major).String()), "major"),
					).
					Value(&releaseType),
			),
		).WithTheme(getFormTheme())

		err = form.Run()
		if err != nil {
			fmt.Println(warningStyle.Render("Cancelled."))
			os.Exit(130)
		}

		var rt ReleaseType
		switch releaseType {
		case "major":
			rt = Major
		case "feature":
			rt = Feature
		default:
			rt = Patch
		}

		newVersion = latestRelease.Bump(rt)

		// Confirmation for major/feature releases
		if rt == Major || rt == Feature {
			var confirmed bool
			confirmForm := huh.NewForm(
				huh.NewGroup(
					huh.NewConfirm().
						Title(fmt.Sprintf("Are you sure you want to create a %s release?", rt.String())).
						Description("This will create " + newVersion.String()).
						Affirmative("Yes, proceed").
						Negative("Cancel").
						Value(&confirmed),
				),
			).WithTheme(getFormTheme())

			err = confirmForm.Run()
			if err != nil || !confirmed {
				fmt.Println(warningStyle.Render("\n⚠ Release cancelled."))
				os.Exit(0)
			}
		}
	}

	state.NewVersion = newVersion

	// Check if release already exists
	if releaseExists(newVersion) {
		fmt.Println()
		fmt.Println(errorStyle.Render("✗ Release " + newVersion.String() + " already exists!"))
		fmt.Println(mutedStyle.Render("  Choose a different version or delete the existing release first."))
		os.Exit(1)
	}

	// Show preview
	fmt.Println()
	previewBox := boxStyle.Copy().BorderForeground(secondaryColor)
	
	var previewContent string
	if isFirstRelease {
		previewContent = fmt.Sprintf(
			"%s\n\n"+
				"  %s %s\n"+
				"  %s %s\n"+
				"  %s %s",
			titleStyle.Render("📦 First Release!"),
			mutedStyle.Render("Version:"),
			successStyle.Render(newVersion.String()),
			mutedStyle.Render("Type:"),
			infoStyle.Render("🎉 Initial Release"),
			mutedStyle.Render("Tag:"),
			infoStyle.Render(newVersion.String()),
		)
	} else {
		previewContent = fmt.Sprintf(
			"%s\n\n"+
				"  %s %s → %s\n"+
				"  %s %s\n"+
				"  %s %s",
			titleStyle.Render("📦 Release Preview"),
			mutedStyle.Render("Version:"),
			warningStyle.Render(latestRelease.String()),
			successStyle.Render(newVersion.String()),
			mutedStyle.Render("Type:"),
			infoStyle.Render("Version Bump"),
			mutedStyle.Render("Tag:"),
			infoStyle.Render(newVersion.String()),
		)
	}
	fmt.Println(previewBox.Render(previewContent))

	// Final confirmation for first release
	if isFirstRelease {
		var confirmed bool
		confirmForm := huh.NewForm(
			huh.NewGroup(
				huh.NewConfirm().
					Title("Ready to publish your first release?").
					Description("This will create " + newVersion.String() + " and publish to Maven Central").
					Affirmative("🚀 Let's go!").
					Negative("Cancel").
					Value(&confirmed),
			),
		).WithTheme(getFormTheme())

		err = confirmForm.Run()
		if err != nil || !confirmed {
			fmt.Println(warningStyle.Render("\n⚠ Release cancelled."))
			os.Exit(0)
		}
	}

	fmt.Println()
	fmt.Println(boxStyle.Render(titleStyle.Render("🚀 Executing Release")))

	// Execute release steps with error handling
	if !stepUpdateChangelog(state) {
		os.Exit(1)
	}

	if !stepUpdatePom(state) {
		os.Exit(1)
	}

	if !stepStageChanges(state) {
		os.Exit(1)
	}

	if !stepCommit(state) {
		os.Exit(1)
	}

	if !stepPush(state) {
		os.Exit(1)
	}

	if !stepCreateRelease(state) {
		os.Exit(1)
	}

	// Success!
	fmt.Println()
	successBox := boxStyle.Copy().BorderForeground(successColor)
	successMsg := fmt.Sprintf(
		"%s\n\n"+
			"  %s Published to GitHub\n"+
			"  %s Maven Central workflow triggered\n\n"+
			"  %s\n"+
			"  %s",
		titleStyle.Render("🎉 Release Successful!"),
		checkmarkStyle.Render("✓"),
		checkmarkStyle.Render("✓"),
		mutedStyle.Render("Monitor the publish workflow at:"),
		infoStyle.Render("https://github.com/paragon-intelligence/agentle4j/actions"),
	)
	fmt.Println(successBox.Render(successMsg))

	// Sleep briefly so the user sees the success message
	time.Sleep(500 * time.Millisecond)
}
