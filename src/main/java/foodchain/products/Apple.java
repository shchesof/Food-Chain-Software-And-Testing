package foodchain.products;

import foodchain.states.GrowingState;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class for apple product in simulation.
 */
public class Apple extends Product {

    /**
     * Constructs apple with default parameters.
     */
    public Apple() {
        this.state = new GrowingState();
        this.statesHistory = new ArrayList<>();
        statesHistory.add(this.state.getStateName());
        price = 20;
        name = "Apple";
        demoStorageParameters = new HashMap<>();
        demoProcessorParameters = new HashMap<>();
        currentlyProcessingParties = new ArrayList<>();
        demoSellerParameters = new HashMap<>();
    }
}