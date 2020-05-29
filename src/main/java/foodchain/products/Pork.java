package foodchain.products;

import foodchain.states.AliveState;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class for pork product in simulation.
 */
public class Pork extends Product {

    /**
     * Constructs pork with default parameters.
     */
    public Pork() {
        this.state = new AliveState();
        this.statesHistory = new ArrayList<>();
        statesHistory.add(this.state.getStateName());
        price = 80;
        name = "Pork";
        demoStorageParameters = new HashMap<>();
        demoProcessorParameters = new HashMap<>();
        currentlyProcessingParties = new ArrayList<>();
        demoSellerParameters = new HashMap<>();
    }
}