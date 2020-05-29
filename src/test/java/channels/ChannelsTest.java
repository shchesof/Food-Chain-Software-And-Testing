package channels;

import foodchain.channels.Channel;
import foodchain.channels.PaymentChannel;
import foodchain.channels.SellingChannel;
import foodchain.parties.*;
import foodchain.products.Milk;
import foodchain.products.Product;
import foodchain.transactions.MoneyTransaction;
import foodchain.transactions.ProductTransaction;
import foodchain.transactions.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ChannelsTest {

    private AbstractParty farmer, storage, customer, processor, distributor, seller;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
    }

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

    // ---------- UNIT TESTS ----------
    @Test
    public void makeMoneyTransmission_FarmerSendsMoney_STDOUTContainsWarning() {
        // ARRANGE
        Channel paymentChannel = new PaymentChannel(storage);
        Integer money = 45;
        Transaction moneyTransaction = new MoneyTransaction(storage, farmer, money);
        String expMsg = "Farmer doesn't send money!";

        // ACT
        Transaction result = paymentChannel.makeTransmission(moneyTransaction);

        // ASSERT
        assertNull(result);
        assertTrue(outContent.toString().contains(expMsg));
    }

    @Test
    public void makeProductTransmission_DetectDoubleSpending_MarkPartyAsCheating() {
        // ARRANGE
        Channel sellingChannel = new SellingChannel(storage);
        Product milk = new Milk();
        milk.setIsCurrentlyProcessed(true);
        Farmer senderFarmer = mock(Farmer.class);
        Transaction productTransaction = new ProductTransaction(storage, senderFarmer, milk);

        // ACT
        sellingChannel.makeTransmission(productTransaction);

        // ASSERT
        verify(senderFarmer, times(1)).setDoubleSpending();
        verify(senderFarmer, times(1)).increaseAttempts();
    }

    @Test
    public void makeTransmission_TransmitMoneyTransaction_ReceiverGetsMoneyTransaction() {
        // ARRANGE
        Farmer receiverFarmer = mock(Farmer.class);
        Channel paymentChannel = new PaymentChannel(receiverFarmer);
        MoneyTransaction moneyTransaction = new MoneyTransaction(receiverFarmer, storage, 45);

        // ACT
        paymentChannel.makeTransmission(moneyTransaction);

        // ARRANGE
        verify(receiverFarmer, times(1)).receiveMoney(moneyTransaction);
    }

    @Test
    public void makeProductTransaction_DoubleSpendingDetected_ProductProcessingPartiesListIsCleared() {
        // ARRANGE
        Channel sellingChannel = new SellingChannel(storage);
        Product milk = new Milk();
        ArrayList<Party> parties  = mock(ArrayList.class);
        parties.add(farmer);
        parties.add(storage);
        parties.add(processor);
        milk.setCurrentlyProcessingParties(parties);
        milk.setIsCurrentlyProcessed(true);
        Transaction productTransaction = new ProductTransaction(storage, farmer, milk);

        // ACT
        sellingChannel.makeTransmission(productTransaction);

        // ARRANGE
        verify(parties, times(1)).clear();
        assertFalse(milk.getCurrentlyProcessingParties().contains(farmer));
        assertFalse(milk.getCurrentlyProcessingParties().contains(storage));
        assertFalse(milk.getCurrentlyProcessingParties().contains(processor));
    }
}