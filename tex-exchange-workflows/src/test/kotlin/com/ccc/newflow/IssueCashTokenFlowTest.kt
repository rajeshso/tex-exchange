package com.ccc.newflow

import com.ccc.flow.CashTokenFlow
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.GBP
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class IssueCashTokenFlowTest   {

    /**
     * Create Mock Network
     */
    private val network = MockNetwork(
        MockNetworkParameters(
            cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                TestCordapp.findCordapp("com.ccc.contract"),
                TestCordapp.findCordapp("com.ccc.flow")
            )
        )
    )
    private val dealerNodeOne = network.createNode(CordaX500Name("dealerNodeOne", "", "GB"))
    private val dealerNodeTwo = network.createNode(CordaX500Name("dealerNodeTwo", "", "GB"))
    private val bankOfEngland = network.createNode(CordaX500Name("BankOfEngland", "London", "GB"))
    private val partyNodeMap = HashMap<StartedMockNode,Party>()

    init {
        partyNodeMap[dealerNodeOne] = dealerNodeOne.info.legalIdentities.first()
        partyNodeMap[dealerNodeTwo] = dealerNodeTwo.info.legalIdentities.first()
        partyNodeMap[bankOfEngland] = bankOfEngland.info.legalIdentities.first()
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `Issue Cash`() {
        //Given
        val amountOfCash = BigDecimal(1000.00)
        val issueCashTokenFlow = CashTokenFlow.IssueCashTokenFlow(amountOfCash)
        //When
        val startFlow = bankOfEngland.startFlow(issueCashTokenFlow)
        //Then
        var orThrow = startFlow.getOrThrow()
        val cashToken = bankOfEngland.services.vaultService.queryBy(FungibleToken::class.java).states[0].state.data
        val amountOfIssuedToken = amountOfCash of GBP issuedBy partyNodeMap[bankOfEngland]!!
        assertEquals(cashToken.amount, amountOfIssuedToken)
    }

    @Test
    @Ignore("Test hangs on notary")
    fun `Issue Cash and Move`() {
        //Given
        val bankOfEnglandCash = BigDecimal(1000.00)
        val dealerOneCash = BigDecimal(250.00)
        val issueCashTokenFlow = CashTokenFlow.IssueCashTokenFlow(bankOfEnglandCash)
        val moveCashTokenFlow = CashTokenFlow.MoveCashTokenFlow(dealerOneCash, partyNodeMap[dealerNodeOne]!!)
        //When
        bankOfEngland.startFlow(issueCashTokenFlow).getOrThrow()
    //    network.runNetwork()
        bankOfEngland.startFlow(moveCashTokenFlow).getOrThrow()
        network.runNetwork()
        //Then
        val cashToken = dealerNodeOne.services.vaultService.queryBy(FungibleToken::class.java).states[0].state.data
        val amountOfIssuedToken = dealerOneCash of GBP issuedBy partyNodeMap[bankOfEngland]!!
        assertEquals(cashToken.amount, amountOfIssuedToken)

    }


}