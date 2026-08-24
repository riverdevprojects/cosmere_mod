package com.cosmere.test;

import java.util.Optional;
import java.util.UUID;

import com.cosmere.Cosmere;
import com.cosmere.InvestitureData;
import com.cosmere.allomancy.AllomanticPhysics;
import com.cosmere.allomancy.MetalScanner;
import com.cosmere.allomancy.MetalTarget;
import com.cosmere.crafting.AlloyRecipe;
import com.cosmere.crafting.AlloyRecipes;
import com.cosmere.hemalurgy.HemalurgyData;
import com.cosmere.hemalurgy.PlacedSpike;
import com.cosmere.hemalurgy.SpikeSlot;
import com.cosmere.hemalurgy.StolenAttribute;
import com.cosmere.item.MetalmindData;
import com.cosmere.metal.Metal;
import com.cosmere.metal.Mineral;
import com.cosmere.registry.ModBlocks;
import com.cosmere.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Tests for the rules that are easy to break by accident.
 *
 * <p>These run inside a real server, which is the point: they catch a metal added to the enum
 * without an item, an alloy recipe that no longer matches, and the Identity rule on metalminds,
 * none of which a compile would notice. Run them with {@code ./gradlew runGameTestServer}.
 */
@GameTestHolder(Cosmere.MODID)
@PrefixGameTestTemplate(false)
public class CosmereGameTests {
    /** Every metal in the enum must actually have the items the rest of the mod assumes. */
    @GameTest(template = "test_platform")
    public static void everyMetalHasItems(GameTestHelper helper) {
        for (Metal metal : Metal.values()) {
            if (metal.hasOwnIngot() && ModItems.INGOTS.get(metal) == null) {
                helper.fail("no ingot registered for " + metal.id());
            }
            if (metal.hasOwnNugget() && ModItems.NUGGETS.get(metal) == null) {
                helper.fail("no nugget registered for " + metal.id());
            }
            if (ModItems.SPIKES.get(metal) == null) {
                helper.fail("no spike registered for " + metal.id());
            }
            if (ModItems.RINGS.get(metal) == null || ModItems.BRACERS.get(metal) == null) {
                helper.fail("no metalmind registered for " + metal.id());
            }
            if (metal != Metal.LERASIUM && ModItems.VIALS.get(metal) == null) {
                helper.fail("no vial registered for " + metal.id());
            }
            if (metal.hasOwnOre() && ModBlocks.DEEPSLATE_METAL_ORES.get(metal) == null) {
                helper.fail("no deepslate ore registered for " + metal.id());
            }
        }
        for (Mineral mineral : Mineral.values()) {
            if (ModItems.MINERAL_INGOTS.get(mineral) == null || ModBlocks.MINERAL_ORES.get(mineral) == null) {
                helper.fail("incomplete registration for mineral " + mineral.id());
            }
        }
        helper.succeed();
    }

    /** Steel is three iron and a coal, in any arrangement, and nothing else. */
    @GameTest(template = "test_platform")
    public static void alloyTableMakesSteel(GameTestHelper helper) {
        SimpleContainer crucible = new SimpleContainer(4);
        crucible.setItem(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 3));
        crucible.setItem(1, new ItemStack(net.minecraft.world.item.Items.COAL, 1));

        AlloyRecipe recipe = AlloyRecipes.find(crucible, 4);
        if (recipe == null) {
            helper.fail("three iron and a coal did not match the steel recipe");
            return;
        }
        if (!recipe.result().is(ModItems.INGOTS.get(Metal.STEEL).get())) {
            helper.fail("steel recipe produced " + recipe.result());
        }

        // A stray ingredient must break the match, or the table would accept anything.
        crucible.setItem(2, new ItemStack(net.minecraft.world.item.Items.DIRT, 1));
        if (AlloyRecipes.find(crucible, 4) != null) {
            helper.fail("crucible matched a recipe with dirt in it");
        }
        helper.succeed();
    }

    /** A keyed metalmind belongs to one Feruchemist; an unkeyed one belongs to anybody. */
    @GameTest(template = "test_platform")
    public static void metalmindsRespectIdentity(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        UUID thief = UUID.randomUUID();

        MetalmindData keyed = MetalmindData.empty(100.0F).store(10.0F, owner, false);
        if (!keyed.canBeTappedBy(owner)) {
            helper.fail("owner cannot tap their own metalmind");
        }
        if (keyed.canBeTappedBy(thief)) {
            helper.fail("a keyed metalmind was tappable by someone else");
        }

        MetalmindData unkeyed = MetalmindData.empty(100.0F).store(10.0F, owner, true);
        if (!unkeyed.canBeTappedBy(thief)) {
            helper.fail("an unkeyed metalmind refused a stranger");
        }

        // Draining a metalmind completely releases the Identity so it can be re-keyed.
        if (!keyed.tap(10.0F).isUnkeyed()) {
            helper.fail("a fully drained metalmind kept its Identity");
        }
        helper.succeed();
    }

    /** A steel spike grants the Allomancy it stole, and an aluminum spike takes everything away. */
    @GameTest(template = "test_platform")
    public static void spikesGrantAndVoidPowers(GameTestHelper helper) {
        InvestitureData data = new InvestitureData();
        if (data.canBurn(Metal.PEWTER)) {
            helper.fail("a blank spiritweb could burn pewter");
        }

        data.hemalurgy().add(new PlacedSpike(Metal.STEEL, SpikeSlot.CHEST,
                Optional.of(new StolenAttribute(StolenAttribute.Kind.ALLOMANTIC_POWER, Optional.of(Metal.PEWTER), 0.7F))));
        if (!data.canBurn(Metal.PEWTER)) {
            helper.fail("a charged steel spike did not grant its Allomancy");
        }

        data.hemalurgy().add(new PlacedSpike(Metal.ALUMINUM, SpikeSlot.SPINE, Optional.empty()));
        if (data.canBurn(Metal.PEWTER)) {
            helper.fail("an aluminum spike failed to void the recipient");
        }
        helper.succeed();
    }

    /** The body must have room for more spikes than it can survive, or the limit is unreachable. */
    @GameTest(template = "test_platform")
    public static void bodyHasRoomForTheLethalLimit(GameTestHelper helper) {
        if (SpikeSlot.totalCapacity() <= HemalurgyData.LETHAL_SPIKE_COUNT) {
            helper.fail("total slot capacity " + SpikeSlot.totalCapacity()
                    + " cannot reach the lethal limit of " + HemalurgyData.LETHAL_SPIKE_COUNT);
        }
        // Mental spikes belong in the head, not the legs.
        if (SpikeSlot.LEFT_LEG.accepts(Metal.COPPER)) {
            helper.fail("a leg accepted a mental spike");
        }
        if (!SpikeSlot.LEFT_EAR.accepts(Metal.COPPER)) {
            helper.fail("an ear refused a mental spike");
        }
        helper.succeed();
    }

    /**
     * The Well of Ascension template and its structure entry must both exist. A structure that
     * fails to load is silent in normal play -- it simply never generates -- so it is checked here.
     */
    @GameTest(template = "test_platform")
    public static void wellOfAscensionLoads(GameTestHelper helper) {
        net.minecraft.resources.ResourceLocation id =
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Cosmere.MODID, "well_of_ascension");

        Optional<net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate> template =
                helper.getLevel().getStructureManager().get(id);
        if (template.isEmpty()) {
            helper.fail("the Well of Ascension structure template did not load");
            return;
        }
        if (template.get().getSize().getY() < 5) {
            helper.fail("the Well of Ascension template is the wrong shape: " + template.get().getSize());
        }

        boolean registered = helper.getLevel().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .containsKey(id);
        if (!registered) {
            helper.fail("the Well of Ascension is not in the structure registry");
        }
        helper.succeed();
    }

    /** Burning drains the reserve, and an empty reserve snuffs the burn. */
    @GameTest(template = "test_platform")
    public static void burningConsumesReserves(GameTestHelper helper) {
        InvestitureData data = new InvestitureData();
        data.grantAllomancy(Metal.STEEL);
        data.addReserve(Metal.STEEL, 1.0F);

        if (!data.setBurning(Metal.STEEL, true)) {
            helper.fail("could not light steel with metal in the stomach and the power to burn it");
        }
        data.consume(Metal.STEEL, 2.0F);
        if (data.isBurning(Metal.STEEL)) {
            helper.fail("steel kept burning after the reserve ran out");
        }
        if (data.setBurning(Metal.STEEL, true)) {
            helper.fail("steel relit with an empty stomach");
        }
        helper.succeed();
    }

    /** Puts a mock player's feet at a relative position in the test platform and returns its eye. */
    private static Vec3 placePlayer(GameTestHelper helper, Player player, double x, double y, double z) {
        Vec3 feet = helper.absoluteVec(new Vec3(x, y, z));
        player.moveTo(feet.x, feet.y, feet.z, 0.0F, 0.0F);
        return player.getEyePosition();
    }

    /** An anchored target is heavier than any Allomancer: the metal wins and you fly instead. */
    @GameTest(template = "test_platform")
    public static void steelpushOnAnchoredTargetThrowsPlayerAway(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 eye = placePlayer(helper, player, 2.5D, 1.0D, 2.5D);

        // The anchor sits five blocks out along +X, level with the Allomancer's eyes.
        MetalTarget target = MetalTarget.ofBlock(BlockPos.containing(eye.add(5.0D, 0.0D, 0.0D)), true, 100.0F);
        AllomanticPhysics.apply(player, target, 1.0F, false);

        Vec3 delta = player.getDeltaMovement();
        if (delta.x >= 0.0D) {
            helper.fail("a Steelpush on an anchored target should throw the player away from it (-X), got " + delta);
        }
        helper.succeed();
    }

    /** The same anchor, but Ironpulled: the Allomancer is dragged toward the metal instead. */
    @GameTest(template = "test_platform")
    public static void ironpullOnAnchoredTargetPullsPlayerToward(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 eye = placePlayer(helper, player, 2.5D, 1.0D, 2.5D);

        MetalTarget target = MetalTarget.ofBlock(BlockPos.containing(eye.add(5.0D, 0.0D, 0.0D)), true, 100.0F);
        AllomanticPhysics.apply(player, target, 1.0F, true);

        Vec3 delta = player.getDeltaMovement();
        if (delta.x <= 0.0D) {
            helper.fail("an Ironpull on an anchored target should pull the player toward it (+X), got " + delta);
        }
        helper.succeed();
    }

    /** A dropped item is far lighter than the Allomancer: Pushing it sends it flying, not you. */
    @GameTest(template = "test_platform")
    public static void steelpushOnLightEntitySendsItAway(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 eye = placePlayer(helper, player, 2.5D, 1.0D, 2.5D);

        Vec3 coinPos = eye.add(3.0D, 0.0D, 0.0D);
        ItemEntity coin = new ItemEntity((ServerLevel) player.level(), coinPos.x, coinPos.y, coinPos.z,
                new ItemStack(Items.IRON_NUGGET));
        coin.setDeltaMovement(Vec3.ZERO);
        // Freshly spawned, mid-air: not resting on the ground, so this cannot anchor.
        coin.setOnGround(false);
        player.level().addFreshEntity(coin);

        MetalTarget target = MetalTarget.ofEntity(coin, false, MetalScanner.entityMetalWeight(coin));
        AllomanticPhysics.apply(player, target, 1.0F, false);

        if (coin.getDeltaMovement().x <= 0.0D) {
            helper.fail("a Steelpush on a light free entity should send it away from the player (+X), got "
                    + coin.getDeltaMovement());
        }
        if (player.getDeltaMovement().lengthSqr() > 1.0E-9D) {
            helper.fail("pushing a light entity should not recoil the player, got " + player.getDeltaMovement());
        }
        helper.succeed();
    }

    /** The same dropped item, Ironpulled: it flies to the player instead of away. */
    @GameTest(template = "test_platform")
    public static void ironpullOnLightEntityBringsItCloser(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 eye = placePlayer(helper, player, 2.5D, 1.0D, 2.5D);

        Vec3 coinPos = eye.add(3.0D, 0.0D, 0.0D);
        ItemEntity coin = new ItemEntity((ServerLevel) player.level(), coinPos.x, coinPos.y, coinPos.z,
                new ItemStack(Items.IRON_NUGGET));
        coin.setDeltaMovement(Vec3.ZERO);
        coin.setOnGround(false);
        player.level().addFreshEntity(coin);

        MetalTarget target = MetalTarget.ofEntity(coin, false, MetalScanner.entityMetalWeight(coin));
        AllomanticPhysics.apply(player, target, 1.0F, true);

        if (coin.getDeltaMovement().x >= 0.0D) {
            helper.fail("an Ironpull on a light free entity should bring it toward the player (-X), got "
                    + coin.getDeltaMovement());
        }
        helper.succeed();
    }

    /**
     * A resting ingot Pushed at a shallow angle skids along the ground instead of anchoring --
     * the documented behaviour of {@link AllomanticPhysics#SLIDE_ANGLE_DEGREES}.
     */
    @GameTest(template = "test_platform")
    public static void restingIngotSlidesAtShallowAngle(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 eye = placePlayer(helper, player, 2.5D, 1.0D, 2.5D);

        double angle = AllomanticPhysics.SLIDE_ANGLE_DEGREES - 5.0D;
        double radius = 4.0D;
        double dx = radius * Math.cos(Math.toRadians(angle));
        double dy = -radius * Math.sin(Math.toRadians(angle));
        Vec3 desiredTargetPos = eye.add(dx, dy, 0.0D);

        ItemEntity ingot = new ItemEntity((ServerLevel) player.level(), 0.0D, 0.0D, 0.0D,
                new ItemStack(Items.IRON_NUGGET));
        double bbHalf = ingot.getBbHeight() * 0.5D;
        Vec3 ingotPos = desiredTargetPos.subtract(0.0D, bbHalf, 0.0D);
        ingot.setPos(ingotPos.x, ingotPos.y, ingotPos.z);
        ingot.setDeltaMovement(Vec3.ZERO);
        ingot.setOnGround(true);
        player.level().addFreshEntity(ingot);

        MetalTarget target = MetalTarget.ofEntity(ingot, false, MetalScanner.entityMetalWeight(ingot));
        AllomanticPhysics.apply(player, target, 1.0F, false);

        if (ingot.getDeltaMovement().x <= 0.0D) {
            helper.fail("a shallow-angle Push on a resting ingot should slide it away, got " + ingot.getDeltaMovement());
        }
        if (player.getDeltaMovement().lengthSqr() > 1.0E-9D) {
            helper.fail("a shallow-angle Push should not anchor and recoil the player, got " + player.getDeltaMovement());
        }
        helper.succeed();
    }

    /**
     * The same resting ingot Pushed at a steep angle anchors instead: the ground takes the
     * force and the Allomancer is thrown, while the ingot itself does not move.
     */
    @GameTest(template = "test_platform")
    public static void restingIngotAnchorsAtSteepAngle(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 eye = placePlayer(helper, player, 2.5D, 1.0D, 2.5D);

        double angle = AllomanticPhysics.SLIDE_ANGLE_DEGREES + 5.0D;
        double radius = 4.0D;
        double dx = radius * Math.cos(Math.toRadians(angle));
        double dy = -radius * Math.sin(Math.toRadians(angle));
        Vec3 desiredTargetPos = eye.add(dx, dy, 0.0D);

        ItemEntity ingot = new ItemEntity((ServerLevel) player.level(), 0.0D, 0.0D, 0.0D,
                new ItemStack(Items.IRON_NUGGET));
        double bbHalf = ingot.getBbHeight() * 0.5D;
        Vec3 ingotPos = desiredTargetPos.subtract(0.0D, bbHalf, 0.0D);
        ingot.setPos(ingotPos.x, ingotPos.y, ingotPos.z);
        ingot.setDeltaMovement(Vec3.ZERO);
        ingot.setOnGround(true);
        player.level().addFreshEntity(ingot);

        MetalTarget target = MetalTarget.ofEntity(ingot, false, MetalScanner.entityMetalWeight(ingot));
        AllomanticPhysics.apply(player, target, 1.0F, false);

        if (player.getDeltaMovement().y <= 0.0D) {
            helper.fail("a steep-angle Push on a resting ingot should anchor and throw the player upward, got "
                    + player.getDeltaMovement());
        }
        if (ingot.getDeltaMovement().lengthSqr() > 1.0E-9D) {
            helper.fail("a steep-angle Push should anchor the ingot in place, but it moved: " + ingot.getDeltaMovement());
        }
        helper.succeed();
    }
}
