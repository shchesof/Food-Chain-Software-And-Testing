package parties;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import foodchain.parties.*;
import foodchain.products.*;
import foodchain.states.AliveState;
import foodchain.states.PackedState;
import foodchain.states.RawState;
import foodchain.states.StoredState;
import foodchain.transactions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;
import static foodchain.parties.Data.*;
import static org.mockito.Mockito.*;

public class PartiesTest {

    private AbstractParty farmer, storage, customer, processor, distributor, seller;
    // to test output
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    public void setUpParties() {
        farmer = new Farmer();
        storage= new Storage();
        processor = new Processor();
        distributor = new Distributor();
        seller = new Seller();
        customer = new Customer();
        storage.setNext(farmer);
        processor.setNext(storage);
        distributor.setNext(processor);
        seller.setNext(distributor);
        customer.setNext(seller);
    }

    @BeforeEach
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    // ---------- UNIT TESTS ----------

    @Test
    public void prepareProductToNextStage_AlivePork_PreparePorkWasInvoked() {
        // ARRANGE
        Pork pork = new Pork();
        AliveState state = mock(AliveState.class);
        pork.setState(state);

        // ACT
        storage.prepareProductToNextStage(pork);

        // ASSERT
        verify(state, times(1)).prepare(pork);
    }

    @Test
    public void makeTransaction_MakeMoneyTransaction_ListOfTransactionsHasCorrectInfo() {
        // ARRANGE
        farmer.setCurrentRequestedProduct(new Milk());
        Integer money = 45; // correct money amount
        
        // ACT
        storage.makeTransaction(money);
        Transaction moneyTransaction = storage.getOwnTransactionsList().get(0);
        
        // ASSERT
        assertNotNull(moneyTransaction);
        assertTrue(moneyTransaction.isSuccessful());
        assertEquals(((MoneyTransaction)moneyTransaction).getMoneyAmount(),45);
    }

    @Test
    public void receiveProduct_StorageReceivesPork_PorkParametersAreCorrect() {
        // ARRANGE
        Product pork = new Pork();
        pork.setState(new RawState());
        ProductTransaction productTransaction = new ProductTransaction(storage, farmer, pork);
        int expHumidity = PORK_STORAGE_HUMIDITY;
        int expTemp = PORK_STORAGE_TEMPERATURE;
        int expTime = PORK_STORAGE_TIME;

        // ACT
        storage.receiveProduct(productTransaction);
        Pork storedPork = (Pork)storage.getProductsList().get(0);
        // extract parameters
        ImmutableSet<String> keys = storedPork.getStorageParameters().keySet();

        // ASSERT
        assertNotNull(storedPork);
        assertEquals(storedPork.getStorageParameters().get(keys.toArray()[0]), expHumidity);
        assertEquals(storedPork.getStorageParameters().get(keys.toArray()[1]), expTemp);
        assertEquals(storedPork.getStorageParameters().get(keys.toArray()[2]), expTime);
        assertEquals(storedPork.getState().getStateName(), "Stored");
    }

    @Test
    public void receiveMoney_CustomerReceiveMoney_STDOUTContainsWarning() {
        // ARRANGE
        Integer money = 45;
        MoneyTransaction moneyTransaction = new MoneyTransaction(customer, seller, money);
        String expectedMessage = "Customer doesn't receive money, but pays!";

        // ACT
        customer.receiveMoney(moneyTransaction);

        // ASSERT
        assertTrue(outContent.toString().contains(expectedMessage));
    }

    @Test
    public void makeTransaction_MakeProductTransaction_ProductIsMarkedAsCurrentlyProcessed() {
        // ARRANGE
        Product apple = new Apple();
        farmer.setMoneyReceived(true);

        // ACT
        farmer.makeTransaction(storage, apple);

        // ASSERT
        assertTrue(apple.isIsCurrentlyProcessed());
    }

    @Test
    public void makeRequest_PartyDoesNotHaveProduct_ProductIsMadeByFoodFactory() {
        // ARRANGE
        String productName = "milk";
        FoodFactory factory = mock(FoodFactory.class);
        seller.setFoodFactory(factory);

        // ACT
        customer.makeRequest(productName);

        // ASSERT
        verify(factory, times(1)).makeProduct(productName);
    }

    // ---------- PROCESS TESTS ----------

    @Test
    public void notEnoughMoneyForProduct_STDOUTContainsWarning() {
        // ARRANGE
        String productName = "milk";
        // correct price is 45
        Integer moneyForProduct = 40;
        Product alreadyExistingMilk = new Milk();
        alreadyExistingMilk.setState(new StoredState());
        // communication only between customer and seller is enough for the test
        seller.addProductToList(alreadyExistingMilk);
        String expMsg = "Not enough money!";

        // ACT
        customer.makeRequest(productName);
        customer.makeTransaction(moneyForProduct);

        // ASSERT
        assertTrue(outContent.toString().contains(expMsg));
        assertNull(customer.getProductsList());
    }

    @Test
    public void enoughMoneyForProductIsSent_PartyReceivesProduct() {
        // ARRANGE
        String productName = "milk";
        Integer moneyForProduct = 45;
        Product alreadyExistingMilk = new Milk();
        alreadyExistingMilk.setState(new StoredState());
        seller.addProductToList(alreadyExistingMilk);

        // ACT
        customer.makeRequest(productName);
        customer.makeTransaction(moneyForProduct);

        // ASSERT
        assertTrue(customer.getProductsList().contains(alreadyExistingMilk));
    }

    @Test
    public void enoughMoneyForProductIsSent_CorrectInfoAboutOwnTransaction() {
        // ARRANGE
        String productName = "milk";
        Integer moneyForProduct = 45;
        Product alreadyExistingMilk = new Milk();
        alreadyExistingMilk.setState(new PackedState());
        seller.addProductToList(alreadyExistingMilk);

        // ACT
        customer.makeRequest(productName);
        customer.makeTransaction(moneyForProduct);
        // get(0) is MoneyTransaction
        ProductTransaction productTransaction = (ProductTransaction)seller.getOwnTransactionsList().get(1);

        // ASSERT
        assertTrue(productTransaction.isSuccessful());
        assertEquals(productTransaction.getProduct(), alreadyExistingMilk);
        assertEquals(productTransaction.getReceiver(), customer);
        assertEquals(productTransaction.getTransactionFlag(), "PRODUCT");
    }

    // TEST FAILS - ERROR DETECTED
    // Expected :1
    // Actual   :2
    @Test
    public void enoughMoneyForProductIsSent_ListOfAllTransactionsContainsProductTransaction() {
        // ARRANGE
        String productName = "milk";
        Integer moneyForProduct = 45;
        Product alreadyExistingMilk = new Milk();
        alreadyExistingMilk.setState(new PackedState());
        seller.addProductToList(alreadyExistingMilk);
        int expSize = 2; // moneyTr + productTr

        // ACT
        customer.makeRequest(productName);
        customer.makeTransaction(moneyForProduct);

        // ASSERT
        assertEquals(customer.getTransactionsList().size(), expSize);
    }

    @Test
    public void noDoubleSpendingTransmission_NoDoubleSpendingIsDetected(){
        // ARRANGE
        String productName = "milk";
        Integer moneyForProduct = 45;
        Product alreadyExistingMilk = new Milk();
        alreadyExistingMilk.setState(new PackedState());
        storage.addProductToList(alreadyExistingMilk);
        String expMsg = "ATTEMPT TO COMMIT DOUBLE SPENDING";
        Integer expDoubleSpendingAttempts = 0;

        // ACT
        customer.makeRequest(productName);
        customer.makeTransaction(moneyForProduct);

        // ASSERT
        assertFalse(outContent.toString().contains(expMsg));
        assertFalse(seller.isAttemptToDoubleSpend());
        assertEquals(expDoubleSpendingAttempts, seller.getAttemptsNumber());
    }

    @Test
    public void detectDoubleSpending_DoubleSpendingIsDetected(){
        // ARRANGE
        String productName = "milk";
        Integer moneyForProduct = 45;
        Product alreadyExistingMilk = new Milk();
        alreadyExistingMilk.setState(new PackedState());
        storage.addProductToList(alreadyExistingMilk);
        String expMsg = "ATTEMPT TO COMMIT DOUBLE SPENDING";
        Integer expDoubleSpendingAttempts = 1;

        // ACT
        customer.makeRequest(productName);
        customer.makeTransaction(moneyForProduct);
        customer.makeRequest(productName);
        customer.makeTransaction(moneyForProduct);

        // ASSERT
        assertTrue(outContent.toString().contains(expMsg));
        assertTrue(seller.isAttemptToDoubleSpend());
        assertEquals(expDoubleSpendingAttempts, seller.getAttemptsNumber());
    }

    @Test
    public void createNewProduct_ProductStateHistoryContainsAllStates(){
        // ARRANGE
        Integer moneyForProduct = 45;
        String productName = "milk";
        List<String> expStatesHistory = new ArrayList<String>(List.of("Collected",
                                                                "Stored",
                                                                "Processed",
                                                                "Delivered",
                                                                "Packed",
                                                                "Sold"));

        // ACT
        customer.makeRequest(productName);
        customer.makeTransaction(moneyForProduct);

        // ASSERT
        assertEquals(expStatesHistory, customer.getProductsList().get(0).getStatesHistory());
    }

    @Test
    public void notEnoughMoney_TransactionMarkedAsFailed(){
        // ARRANGE
        Integer moneyForProduct = 40;
        String productName = "milk";

        // ACT
        customer.makeRequest(productName);
        customer.makeTransaction(moneyForProduct);

        // ASSERT
        assertFalse(customer.getOwnTransactionsList().get(0).isSuccessful());
    }
}