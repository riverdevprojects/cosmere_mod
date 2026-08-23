# Cosmere

A Minecraft **1.21.1** NeoForge mod about the worlds of Brandon Sanderson.

This release is **Scadrial**: all three metallic arts, the mists, and the things that live in
them. Other planets are planned; see [Dimensions](#dimensions).

```
./gradlew runClient        # play it
./gradlew runGameTestServer # run the tests
./gradlew build            # jar in build/libs/
```

---

## Controls

| Key | What it does |
| --- | --- |
| **R** | Burn window. One row per metal you can use: left half burns, right half stores or taps. |
| **F** | Arms Allomancy. While armed, attack Steelpushes and use Ironpulls. |
| **Caps Lock** | Flare. Hotter and stronger while held, and burns through metal three times as fast. |

Arming does **not** light anything — you still choose what is burning in the burn window. What
arming does is decide whether a click is Allomancy or the ordinary thing.

---

## Getting powers

You start with none. There are three ways in:

- **Snapping.** Nearly dying out under the mists at night sometimes tears something loose, and
  you come back a Misting able to burn one metal. This is the intended first power.
- **A bead of lerasium.** Swallow it and you are Mistborn — all sixteen, plus the god metals.
  Beads turn up in the Well of Ascension and nowhere else.
- **Hemalurgy.** Take a power off something else. See below.

Once you can burn a metal, you still need the metal: drink a **vial**, which puts flakes in your
stomach where Allomancy can reach them. Vials are a glass bottle and three nuggets.

---

## Allomancy

Sixteen metals in four quadrants, plus five god metals.

|  | Pure (pull) | Alloy (push) |
| --- | --- | --- |
| **Physical** | **Iron** — Lurcher. Pulls metal, shows blue lines. | **Steel** — Coinshot. Pushes metal, shows blue lines. |
| | **Tin** — Tineye. Enhanced senses; blinded by daylight without a blindfold. | **Pewter** — Thug. Strength and endurance, on credit. |
| **Mental** | **Zinc** — Rioter. Inflames nearby emotions. | **Brass** — Soother. Dampens them. |
| | **Copper** — Smoker. Hides you from Seekers. | **Bronze** — Seeker. Hears Allomancy nearby. |
| **Temporal** | **Cadmium** — Pulser. Time crawls. | **Bendalloy** — Slider. Time races for you and drags for everyone else. |
| | **Gold** — Augur. Shows who you might have been. | **Electrum** — Oracle. Shows futures you have not taken. |
| **Enhancement** | **Chromium** — Leecher. Strips another Allomancer's metals at a touch. | **Nicrosil** — Nicroburst. Dumps everything they are burning at once. |
| | **Aluminum** — Gnat. Scours every other metal out of you. | **Duralumin** — Gnat. Burns everything else at once, then goes out. |

**God metals**: atium (see a second ahead — nothing living can touch you), lerasium (become
Mistborn), malatium (see who someone else used to be), harmonium (stands in for any metal),
trellium (shields against emotional Allomancy).

### Pushing and Pulling

Burning iron or steel draws a blue line to every piece of metal in range. Arm with **F**, then
hold attack to Push or use to Pull. Something always moves:

- Push on something heavier than you — an iron door, a wall of metal, a bolted fixture — and
  **you** are thrown the other way. This is how you fly.
- Push on something lighter — a dropped coin, an ingot on the floor — and **it** flies.

An ingot lying on the ground is the interesting case. Push along it at a shallow angle
(within **20°** of the ground plane) and it skids away. Push down into it more steeply and the
ground behind it takes the force: the ingot anchors and you go up.

Push with nothing in front of you and you flick whatever metal is in your hand out at speed.
That is what coins are for.

An iron pressure plate can be Pushed from across a room, which is how you open a door you are
not standing next to. **Glass and obsidian cannot be Pushed or Pulled at all** — that is exactly
why an assassin carries them.

### Pewter drag

While pewter burns, damage does not land. It accumulates. The moment pewter runs out — or you
snuff it — the whole bill arrives at once, along with weakness and slowness. The HUD shows a
second bar for what your body actually has left underneath.

---

## Feruchemy

Storing an attribute costs you it now; tapping gives it back compressed. Metalminds come as
**rings** (small, discreet) and **bracers** (large, obvious). Carry one, then set it to Storing
or Tapping in the burn window.

Filling a metalmind imprints your **Identity** on it, and only you can tap it afterwards — unless
you fill it while storing **aluminum**, which strips Identity and leaves it **unkeyed** for
anyone. Draining a metalmind to empty releases the Identity again.

Iron stores weight, steel speed, tin senses, pewter strength; zinc mental speed, brass warmth,
copper memory, bronze wakefulness; cadmium breath, bendalloy calories, gold health, electrum
determination; chromium fortune, nicrosil Investiture, aluminum Identity, duralumin Connection.

---

## Hemalurgy

Hemalurgy takes a power off one person and gives it to another, and loses about a third of it on
the way.

1. Craft a **spike** — two nuggets. The metal decides what it can steal.
2. Place a **Hemalurgic Table**.
3. Lead the victim to it on a **lead** and strike the table with a blank spike. The spike comes
   away charged. The victim does not get up.
4. Strike the table again with the charged spike and no victim present to drive it into
   yourself — or open a **Jar of Spikes**, pick a spike onto the cursor, and click a place on
   the body diagram to choose exactly where it goes.

Placement matters: mental spikes take in the head, physical ones in the arms, ribs and legs.
The chest and spine take anything. Every spike hurts, and more of them hurt more.

Past **47 spikes** the spiritweb comes apart. You die, and because your Connection to your bed is
shredded along with everything else, you respawn at world spawn.

Spikes bleed their charge away when carried loose. A **Jar of Spikes** keeps them in blood, which
is what the Steel Ministry did and why you find their jars in ruins.

Wearing an **aluminum spike** voids your own powers entirely. A **zinc spike** or a tapped
aluminummind armours your spiritweb: no Soothing, no Rioting, no new spikes.

---

## The mists

The mists come out at night in the Overworld wherever the sky is visible. They **pull away** from
anyone carrying Hemalurgic spikes, so a spiked player walks in a visible clear bubble.

Out of them come **mistwraiths**: harmless, boneless things that shamble and eat the dead. Drive
two charged spikes of the same metal into one, one per shoulder, and it wakes up as a **kandra**,
bound by Contract to whoever drove in the second spike.

| Spike metal | Blessing | What it is |
| --- | --- | --- |
| Iron | Potency | Massive physical strength |
| Tin | Agility | Speed and dexterity |
| Copper | Presence | Intelligence |
| Zinc | Awareness | Emotional intelligence |

**Koloss** are made, not born: four iron spikes through your own ribs, then a koloss skin laid
over you on a Hemalurgic Table. You become huge, strong, slow, and hard to move. There is no
cure. Wild koloss attack anything they can reach; a Soothing calms one into a guard for about a
minute, and Rioting sets it off again.

---

## Materials

New ores: **tin, zinc, cadmium, aluminum, chromium, atium** (deepslate only, deep and rare), plus
**lead, silver, nickel and bismuth** as alloy ingredients.

Alloys are mixed at a **Metallurgy Table** — four crucible slots, arrangement irrelevant:

| Alloy | Mixture |
| --- | --- |
| Steel | 3 iron + 1 coal |
| Pewter | 3 tin + 1 lead |
| Brass | 2 copper + 2 zinc |
| Bronze | 3 copper + 1 tin |
| Bendalloy | bismuth + lead + tin + cadmium |
| Electrum | 2 gold + 2 silver |
| Duralumin | 3 aluminum + 1 copper |
| Nicrosil | 3 nickel + 1 chromium |
| Malatium | 1 atium + 1 gold |
| Harmonium | 1 lerasium + 1 atium |

Also: **clips** and **boxings** (Scadrian money, which villagers accept and Coinshots throw),
the **blindfold**, glass and obsidian **daggers, swords and axes**, and the **duelling cane**.
Daggers are a stick plus one ingot or glass block, swing fast, and are devastating from behind.

---

## The Well of Ascension

A chamber underground, walls veined with atium, a black pool at its centre. Rare. It is meant to
sit beneath Luthadel; until the city generates, it stands alone.

---

## Dimensions

`com.cosmere.dimension.CosmerePlanets` names Scadrial, Roshar, Nalthis, Sel and Taldain, and
registers none of them. Scadrial currently *is* the Overworld. The enum exists so the rest of the
mod can already ask which planet it is on and get a real answer — adding a world later changes
one key, not fifty call sites.

---

## Configuration

`run/config/cosmere-common.toml`, or the Mods screen in game. Mists on or off, Snapping on or
off, mistwraith spawn rate and cap, Push strength, blue-line range, whether chromium works on
players, and whether over-spiking is lethal.

## Licence

`All Rights Reserved` (see `gradle.properties`). The Cosmere and everything in it belongs to
Brandon Sanderson; this is an unofficial fan project.
