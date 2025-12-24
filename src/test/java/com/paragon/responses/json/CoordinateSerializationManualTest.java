package com.paragon.responses.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.spec.ClickAction;
import com.paragon.responses.spec.ClickButton;
import com.paragon.responses.spec.Coordinate;
import com.paragon.responses.spec.MoveAction;

/**
 * Manual test to verify Coordinate serialization behavior. Run this main method to see the JSON
 * output.
 */
public class CoordinateSerializationManualTest {

  public static void main(String[] args) throws Exception {
    ObjectMapper mapper = ResponsesApiObjectMapper.create();

    // Test MoveAction
    Coordinate coord = Coordinate.Factory.getCoordinate(100, 200);
    MoveAction moveAction = new MoveAction(coord);

    String moveJson = mapper.writeValueAsString(moveAction);
    System.out.println("MoveAction JSON:");
    System.out.println(moveJson);
    System.out.println();

    // Test ClickAction
    ClickAction clickAction = new ClickAction(ClickButton.LEFT, coord);
    String clickJson = mapper.writeValueAsString(clickAction);
    System.out.println("ClickAction JSON:");
    System.out.println(clickJson);
    System.out.println();

    // Test deserialization
    MoveAction deserialized = mapper.readValue(moveJson, MoveAction.class);
    System.out.println("Deserialized MoveAction:");
    System.out.println("  x: " + deserialized.coordinate().x());
    System.out.println("  y: " + deserialized.coordinate().y());
    System.out.println();

    // Test incomplete coordinate (should fail)
    try {
      String incompleteJson = "{\"type\": \"move\", \"x\": 100}";
      mapper.readValue(incompleteJson, MoveAction.class);
      System.out.println("ERROR: Should have thrown exception for incomplete coordinate");
    } catch (Exception e) {
      System.out.println("Correctly threw exception for incomplete coordinate:");
      System.out.println("  " + e.getMessage());
    }
  }
}
