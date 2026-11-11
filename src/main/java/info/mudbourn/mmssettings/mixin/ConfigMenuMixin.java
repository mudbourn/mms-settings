package info.mudbourn.mmssettings.mixin;

import info.mudbourn.mmssettings.opac.OpacScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.pac.client.gui.ConfigMenu;

@Mixin(ConfigMenu.class)
public abstract class ConfigMenuMixin extends Screen {
    protected ConfigMenuMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void mmsSettings$addViewButton(CallbackInfo ci) {
        addRenderableWidget(
            Button.builder(
                Component.translatable("mms_settings.opac.view_button"),
                button -> Minecraft.getInstance().setScreen(OpacScreens.create(this))
            )
                .bounds(6, 6, 80, 20)
                .build()
        );
    }
}
