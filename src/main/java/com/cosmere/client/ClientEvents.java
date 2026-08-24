package com.cosmere.client;

import com.cosmere.Cosmere;
import com.cosmere.InvestitureData;
import com.cosmere.client.screen.BurnWindowScreen;
import com.cosmere.metal.Metal;
import com.cosmere.network.c2s.AllomanticActionPayload;
import com.cosmere.network.c2s.SetFlarePayload;
import com.cosmere.network.c2s.SetPushPullArmedPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Turning key presses into intent, and refusing to let vanilla have the click when Allomancy
 * has claimed it.
 *
 * <p>The client keeps a little state -- was F armed last tick, was the attack button down --
 * only so it can send edges rather than a packet every tick. All of it is re-validated server
 * side.
 */
@EventBusSubscriber(modid = Cosmere.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private static boolean armed;
    private static boolean flaring;
    // Raw button edges, tracked independent of Allomancy, so Nicroburst/Leech fire once per
    // physical press even when steel/iron isn't burning.
    private static boolean attackWasDown;
    private static boolean useWasDown;
    // What we last told the server Push/Pull was doing. Only these gate a send.
    private static boolean pushReported;
    private static boolean pullReported;

    public static boolean isArmed() {
        return armed;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            reset();
            return;
        }

        AllomanticLineCache.tick(minecraft);
        ClientInvestitureCache.expirePulses(System.currentTimeMillis() / 50L);

        handleToggleKeys(minecraft);
        handleFlare();
        handlePushPull(minecraft);
    }

    private static void handleToggleKeys(Minecraft minecraft) {
        while (ModKeyMappings.TOGGLE_ARMED.consumeClick()) {
            armed = !armed;
            PacketDistributor.sendToServer(new SetPushPullArmedPayload(armed));
            minecraft.player.displayClientMessage(Component.translatable(
                    armed ? "cosmere.message.armed" : "cosmere.message.disarmed"), true);
        }
        while (ModKeyMappings.BURN_WINDOW.consumeClick()) {
            minecraft.setScreen(new BurnWindowScreen());
        }
    }

    private static void handleFlare() {
        boolean down = ModKeyMappings.FLARE.isDown();
        if (down != flaring) {
            flaring = down;
            PacketDistributor.sendToServer(new SetFlarePayload(flaring));
        }
    }

    /**
     * While armed, holding attack Steelpushes and holding use Ironpulls. Chromium and nicrosil
     * ride the same two buttons because they are the same gesture -- reach out and touch
     * someone.
     */
    private static void handlePushPull(Minecraft minecraft) {
        if (!armed) {
            releaseAll();
            return;
        }
        InvestitureData data = ClientInvestitureCache.local();
        ClientTargeting.Pick pick = ClientTargeting.pick(minecraft);

        boolean attackDown = minecraft.options.keyAttack.isDown();
        boolean useDown = minecraft.options.keyUse.isDown();

        // Nicrosil and chromium fire once on the press rather than being held.
        if (attackDown && !attackWasDown && data.isBurning(Metal.NICROSIL) && pick.entityId() >= 0) {
            send(AllomanticActionPayload.Action.NICROBURST, true, pick);
        }
        if (useDown && !useWasDown && data.isBurning(Metal.CHROMIUM) && pick.entityId() >= 0) {
            send(AllomanticActionPayload.Action.LEECH, true, pick);
        }

        // Compare against what the button+metal combination actually wants, not against the raw
        // button state -- otherwise igniting steel/iron while the button is already held never
        // produces an edge, and Push/Pull silently never starts until a fresh press.
        boolean wantsPush = data.isBurning(Metal.STEEL) && attackDown;
        if (wantsPush != pushReported) {
            pushReported = wantsPush;
            send(AllomanticActionPayload.Action.PUSH, wantsPush, pick);
        }
        boolean wantsPull = data.isBurning(Metal.IRON) && useDown;
        if (wantsPull != pullReported) {
            pullReported = wantsPull;
            send(AllomanticActionPayload.Action.PULL, wantsPull, pick);
        }

        attackWasDown = attackDown;
        useWasDown = useDown;
    }

    private static void send(AllomanticActionPayload.Action action, boolean pressed, ClientTargeting.Pick pick) {
        PacketDistributor.sendToServer(new AllomanticActionPayload(action, pressed, pick.entityId(), pick.blockPos()));
    }

    private static void releaseAll() {
        if (pushReported) {
            pushReported = false;
            send(AllomanticActionPayload.Action.PUSH, false, ClientTargeting.Pick.NOTHING);
        }
        if (pullReported) {
            pullReported = false;
            send(AllomanticActionPayload.Action.PULL, false, ClientTargeting.Pick.NOTHING);
        }
    }

    private static void reset() {
        armed = false;
        flaring = false;
        attackWasDown = false;
        useWasDown = false;
        pushReported = false;
        pullReported = false;
        ClientInvestitureCache.clear();
    }

    /**
     * While armed, a click belongs to Allomancy. Vanilla does not get to swing the sword or
     * place the block.
     */
    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!armed) {
            return;
        }
        InvestitureData data = ClientInvestitureCache.local();
        boolean claimsAttack = data.isBurning(Metal.STEEL) || data.isBurning(Metal.NICROSIL);
        boolean claimsUse = data.isBurning(Metal.IRON) || data.isBurning(Metal.CHROMIUM);
        if ((event.isAttack() && claimsAttack) || (event.isUseItem() && claimsUse)) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    private ClientEvents() {
    }
}
