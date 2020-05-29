package states;

import foodchain.products.Apple;
import foodchain.products.Milk;
import foodchain.states.*;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

public class StateTest {

    // ---------- UNIT TESTS ----------

    @Test
    public void prepare_GrowingApple_CollectedApple(){
        // ARRANGE
        Apple apple = new Apple();
        GrowingState stateControl = new GrowingState();
        String expectedAppleState = "Collected";

        // ACT
        stateControl.prepare(apple);
        String realAppleState = apple.getState().getStateName();

        // ASSERT
        assertEquals(expectedAppleState, realAppleState);
    }


    // TEST FAILS - ERROR DETECTED
    // Expected : some kind of exception
    // Actual   : nothing
    @Test
    public void prepare_Milk_throwsException(){
        // ARRANGE
        Milk milk = new Milk();
        RawState stateControl = new RawState();

        // ASSERT
        assertThrows(Exception.class, () -> {
            // ACT
            stateControl.prepare(milk);
        });
    }
}