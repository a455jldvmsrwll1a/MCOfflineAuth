package balls.jl.mcofflineauth.client.mixin;

import balls.jl.mcofflineauth.client.MCOfflineAuthClient;
import balls.jl.mcofflineauth.client.screen.ModScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(at = @At("RETURN"), method = "init")
    protected void init(CallbackInfo ci) {
        if (MCOfflineAuthClient.SHOW_HELP_TOAST) {
            MCOfflineAuthClient.SHOW_HELP_TOAST = false;
            assert this.client != null;
            this.client.getToastManager().add(SystemToast.create(this.client, SystemToast.Type.NARRATOR_TOGGLE, Text.of("Created a new key-pair!"), Text.of("Click the OA button for more info.")));
        }
    }

    @Inject(at = @At("RETURN"), method = "addNormalWidgets")
    private void addModsButton(int y, int spacingY, CallbackInfoReturnable<Integer> cir) {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("OA"), (button) -> {
            Screen currentScreen = MinecraftClient.getInstance().currentScreen;
            MinecraftClient.getInstance().setScreen(new ModScreen(currentScreen));
        }).dimensions(this.width / 2 - 100 - spacingY, y, 20, 20).build());
    }
}