package leji.bbslezy.ui.skin;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.forms.ModelForm;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinSearchTest
{
    @Test
    void testIsAlexNullOrMissing()
    {
        assertFalse(SkinFetcher.isAlex(null));
        assertFalse(SkinFetcher.isAlex(new File("nonexistent_skin.png")));
    }

    @Test
    void testIsPlayerSkinCompatible()
    {
        /* Null form */
        assertFalse(SkinSearchByNickPanel.isPlayerSkinCompatible(null));

        /* Non-model form (e.g. generic Form, block/item forms) */
        Form genericForm = new Form() {};
        assertFalse(SkinSearchByNickPanel.isPlayerSkinCompatible(genericForm));

        /* ModelForm with vanilla player/steve or player/alex */
        ModelForm steve = new ModelForm();
        steve.model.set("player/steve");
        assertTrue(SkinSearchByNickPanel.isPlayerSkinCompatible(steve));

        ModelForm alex = new ModelForm();
        alex.model.set("player/alex");
        assertTrue(SkinSearchByNickPanel.isPlayerSkinCompatible(alex));

        /* ModelForm with empty model (default player) */
        ModelForm empty = new ModelForm();
        empty.model.set("");
        assertTrue(SkinSearchByNickPanel.isPlayerSkinCompatible(empty));

        /* Custom model / custom player rig */
        ModelForm customRig = new ModelForm();
        customRig.model.set("my_custom_player_rig");
        assertTrue(SkinSearchByNickPanel.isPlayerSkinCompatible(customRig));

        /* MobForm */
        assertTrue(SkinSearchByNickPanel.isPlayerSkinCompatible(new MobForm()));
    }
}
