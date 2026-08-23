package com.cosmere.client.hud;

import com.cosmere.metal.Metal;

/**
 * Stand-in glyphs for the Steel Alphabet.
 *
 * <p>The real alphabet is a set of sixteen symbols laid out on the metal table; until the mod
 * ships a font for them, each metal gets a distinct printable character so the Seeker overlay
 * is still readable at a glance. Swapping in a proper glyph font later means changing only this
 * table.
 */
public final class SteelAlphabet {
    public static String glyphFor(Metal metal) {
        return switch (metal) {
            case IRON -> "◤";
            case STEEL -> "◥";
            case TIN -> "◣";
            case PEWTER -> "◢";
            case ZINC -> "◸";
            case BRASS -> "◹";
            case COPPER -> "◺";
            case BRONZE -> "◿";
            case CADMIUM -> "△";
            case BENDALLOY -> "▽";
            case GOLD -> "◇";
            case ELECTRUM -> "◈";
            case ALUMINUM -> "○";
            case DURALUMIN -> "◎";
            case CHROMIUM -> "◐";
            case NICROSIL -> "◑";
            case ATIUM -> "★";
            case LERASIUM -> "☆";
            case MALATIUM -> "✦";
            case HARMONIUM -> "✧";
            case TRELLIUM -> "✶";
        };
    }

    private SteelAlphabet() {
    }
}
