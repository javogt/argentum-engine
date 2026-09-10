package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Kabira Takedown // Kabira Plateau (ZNR #19) — the first spell // land modal double-faced card.
 *
 * Front — {1}{W} Instant: "Kabira Takedown deals damage equal to the number of creatures you
 * control to target creature or planeswalker."
 * Back  — Land: "This land enters tapped." / "{T}: Add {W}."
 *
 * The two faces are two different *kinds* of play, which is what `CardDefinition
 * .modalDoubleFacedLandBack` exists to express: the front is cast (CR 712.11b), the back is played
 * as a land with its face chosen on the way in (CR 712.12). So the card offers exactly one cast and
 * exactly one land play from hand, never a land play of the instant face nor a cast of the land.
 */
class KabiraTakedownScenarioTest : ScenarioTestBase() {

    private val bulwark = CardDefinition.creature(
        name = "Test Bulwark",
        manaCost = ManaCost.parse("{4}"),
        subtypes = setOf(Subtype("Wall")),
        power = 0,
        toughness = 8
    )

    private fun damageOn(game: TestGame, name: String): Int =
        game.state.getEntity(game.findPermanent(name)!!)?.get<DamageComponent>()?.amount ?: 0

    init {
        cardRegistry.register(bulwark)

        context("Kabira Takedown // Kabira Plateau — one card, two kinds of play") {

            test("the card in hand offers a land play of the back face only, plus the instant cast") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Kabira Takedown")
                    // The instant needs a legal target to be offered as a cast at all.
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val actions = game.getLegalActions(1)

                withClue("only the land face may be played as a land (CR 712.12)") {
                    actions.filter { it.actionType == "PlayLand" }
                        .map { it.description }
                        .filter { "Kabira" in it } shouldBe listOf("Play Kabira Plateau")
                }
                withClue("the instant face is castable from hand (CR 712.11b)") {
                    actions.any { it.actionType == "CastSpell" && "Kabira Takedown" in it.description } shouldBe true
                }
            }
        }

        context("Kabira Takedown — the instant face") {

            test("deals damage equal to the number of creatures you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Kabira Takedown")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Test Bulwark")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val victim = game.findPermanent("Test Bulwark")
                victim.shouldNotBeNull()

                game.castSpell(1, "Kabira Takedown", victim).error shouldBe null
                game.resolveStack()

                withClue("two creatures you control, so two damage") {
                    damageOn(game, "Test Bulwark") shouldBe 2
                }
            }

            test("with no creatures you control it deals no damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Kabira Takedown")
                    .withCardOnBattlefield(2, "Test Bulwark")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val victim = game.findPermanent("Test Bulwark")!!

                game.castSpell(1, "Kabira Takedown", victim).error shouldBe null
                game.resolveStack()

                withClue("the opponent's creatures are not yours; the count is zero") {
                    damageOn(game, "Test Bulwark") shouldBe 0
                }
            }
        }

        context("Kabira Plateau — the land face") {

            test("played as a land it enters tapped, back face up, and is no longer the instant") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Kabira Takedown")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val cardId = game.findCardsInHand(1, "Kabira Takedown").single()

                game.execute(PlayLand(game.player1Id, cardId, asBackFace = true)).error shouldBe null

                withClue("the permanent is the back face, not the printed front (CR 712.8f)") {
                    game.findPermanent("Kabira Takedown") shouldBe null
                }
                val land = game.findPermanent("Kabira Plateau")
                land.shouldNotBeNull()
                game.state.getEntity(land)?.get<DoubleFacedComponent>()?.currentFace shouldBe
                    DoubleFacedComponent.Face.BACK
                withClue("\"This land enters tapped.\"") {
                    game.state.getEntity(land)?.has<TappedComponent>() shouldBe true
                }
            }

            test("the land face taps for white") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kabira Plateau")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val land = game.findPermanent("Kabira Plateau")
                land.shouldNotBeNull()
                val manaAbilityId =
                    cardRegistry.requireCard("Kabira Plateau").script.activatedAbilities.first().id

                game.execute(ActivateAbility(game.player1Id, land, manaAbilityId)).error shouldBe null

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                pool?.getAmount(Color.WHITE) shouldBe 1
            }
        }
    }
}
