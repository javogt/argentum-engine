package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Kabira Takedown // Kabira Plateau — Zendikar Rising #19 (canonical printing)
 * Instant // Land · Modal double-faced card
 *
 * Front — Kabira Takedown ({1}{W}, Instant): "Kabira Takedown deals damage equal to the number of
 *   creatures you control to target creature or planeswalker."
 * Back  — Kabira Plateau (Land): "This land enters tapped." / "{T}: Add {W}."
 *
 * One of Zendikar Rising's common spell // land modal DFCs. The two faces are two different *kinds*
 * of play, not two costs for the same one: the front is cast (CR 712.11b), the back is played as a
 * land with the face chosen on the way in (CR 712.12), and choosing one is choosing against the
 * other. Per CR 712.8f only the front face is considered in every zone but the battlefield, so the
 * card is an instant in hand, graveyard and library, and a land only once the back face is played.
 *
 * The damage is counted as the spell resolves, so creatures that die in response shrink it — the
 * same timing as [com.wingedsheep.mtg.sets.definitions.bfz.cards.Outnumber].
 */
private val KabiraTakedownFront = card("Kabira Takedown") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Kabira Takedown deals damage equal to the number of creatures you control to " +
        "target creature or planeswalker."

    spell {
        val victim = target("target creature or planeswalker", Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(DynamicAmounts.creaturesYouControl(), victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "19"
        artist = "Randy Vargas"
        flavorText = "\"Down below's where things go to die.\"\n—Kreq of Sunder Bay"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/366e9845-019d-47cc-adb8-8fbbaad35b6d.jpg?1783929423"

        ruling(
            "2020-09-25",
            "To determine whether it is legal to play a modal double-faced card, consider only the " +
                "characteristics of the face you're playing and ignore the other face's characteristics."
        )
        ruling(
            "2020-09-25",
            "If an effect allows you to play a specific modal double-faced card, you may cast it as " +
                "a spell or play it as a land, as determined by which face you choose to play. If an " +
                "effect allows you to cast (rather than \"play\") a specific modal double-faced card, " +
                "you can't play it as a land."
        )
        ruling(
            "2020-09-25",
            "The mana value of a modal double-faced card is based on the characteristics of the face " +
                "that's being considered. On the stack and battlefield, consider whichever face is up. " +
                "In all other zones, consider only the front face. This is different than how the mana " +
                "value of a transforming double-faced card is determined."
        )
        ruling(
            "2020-09-25",
            "A modal double-faced card can't be transformed or be put onto the battlefield transformed. " +
                "Ignore any instruction to transform a modal double-faced card or to put one onto the " +
                "battlefield transformed."
        )
    }
}

private val KabiraPlateauBack = card("Kabira Plateau") {
    colorIdentity = "W"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n{T}: Add {W}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE, 1)
        manaAbility = true
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "19"
        artist = "Randy Vargas"
        flavorText = "\"Wise adventurers—and clever beasties—stick to the higher ground.\"\n" +
            "—Kreq of Sunder Bay"
        imageUri = "https://cards.scryfall.io/normal/back/3/6/366e9845-019d-47cc-adb8-8fbbaad35b6d.jpg?1783929423"
    }
}

val KabiraTakedown: CardDefinition = CardDefinition.modalDoubleFacedLandBack(
    frontFace = KabiraTakedownFront,
    backFace = KabiraPlateauBack,
)
