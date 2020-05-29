package foodchain.products;

import foodchain.states.CollectedState;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class for milk product in simulation.
 */
public class Milk extends Product {

    /**
     * Constructs milk with default parameters.
     */
    public Milk() {
        this.state = new CollectedState();
        this.statesHistory = new ArrayList<>();
        statesHistory.add(this.state.getStateName());
        price = 45;
        name = "Milk";
        demoStorageParameters = new HashMap<>();
        demoProcessorParameters = new HashMap<>();
        currentlyProcessingParties = new ArrayList<>();
        demoSellerParameters = new HashMap<>();
    }
}