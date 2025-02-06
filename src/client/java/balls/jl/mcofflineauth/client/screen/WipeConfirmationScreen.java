package balls.jl.mcofflineauth.client.screen;

import balls.jl.mcofflineauth.client.ClientKeyPair;
import balls.jl.mcofflineauth.client.MCOfflineAuthClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class WipeConfirmationScreen extends Screen {
    public final Screen parent;

    public WipeConfirmationScreen(Screen parent) {
        super(Text.empty());
        this.parent = parent;
    }

    @Override
    protected void init() {
        assert this.client != null;
        int width = this.client.getWindow().getScaledWidth();
        int height = this.client.getWindow().getScaledHeight();
        ButtonWidget cancelBtnWidget = ButtonWidget.builder(Text.of("Cancel"), (btn) -> close()).dimensions(20, height - this.textRenderer.fontHeight - 20, 80, 20).build();

        ButtonWidget confirmBtnWidget = ButtonWidget.builder(Text.literal("Confirm").formatted(Formatting.RED, Formatting.BOLD), (btn) -> {
            ClientKeyPair.generate();
            MCOfflineAuthClient.SHOW_HELP_TOAST = false;

            this.client.getToastManager().add(SystemToast.create(this.client, SystemToast.Type.NARRATOR_TOGGLE, Text.of("New key-pair created."), Text.of("The old key-pair will no longer work!")));
            close();
        }).tooltip(Tooltip.of(Text.literal("OLD KEY-PAIR WILL NO LONGER WORK!").formatted(Formatting.RED, Formatting.BOLD))).dimensions(width - 80 - 20, height - this.textRenderer.fontHeight - 20, 80, 20).build();

        addDrawableChild(cancelBtnWidget);
        addDrawableChild(confirmBtnWidget);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawText(this.textRenderer, "MC Offline Auth Configuration", 15, 40 - this.textRenderer.fontHeight - 10, 0xFFA719FF, true);
        context.drawText(this.textRenderer, "§lConfirm deletion of the key-pair?§r", 15, 60 - this.textRenderer.fontHeight - 10, 0xFF0000, true);
        context.drawText(this.textRenderer, "After deleting, the key-pair will no longer be usable.", 15, 80 - this.textRenderer.fontHeight - 10, 0xFFFFFFFF, true);
        context.drawText(this.textRenderer, "Servers with your old key §nwill reject you until you bind the new public key§r.", 15, 90 - this.textRenderer.fontHeight - 10, 0xFFFFFFFF, true);
        context.drawText(this.textRenderer, "You can achieve this it two ways:", 15, 100 - this.textRenderer.fontHeight - 10, 0xFFFFFFFF, true);
        context.drawText(this.textRenderer, "§oBefore deleting§r, unbind your user in the server(s): [recommended]", 15, 120 - this.textRenderer.fontHeight - 10, 0xFF38FF74, true);
        context.drawText(this.textRenderer, "Before you create a new key-pair, do \"§i/offauth unbind§r\" then leave,", 15, 130 - this.textRenderer.fontHeight - 10, 0xFFFFFFFF, true);
        context.drawText(this.textRenderer, "then come back to this screen and §oconfirm§r delete the old key-pair,", 15, 140 - this.textRenderer.fontHeight - 10, 0xFFFFFFFF, true);
        context.drawText(this.textRenderer, "then rejoin and bind your new public key using \"§i/offauth bind§r\".", 15, 150 - this.textRenderer.fontHeight - 10, 0xFFFFFFFF, true);
        context.drawText(this.textRenderer, "Otherwise, tell an admin to do it for you:", 15, 170 - this.textRenderer.fontHeight - 10, 0xFF9BC2A7, true);
        context.drawText(this.textRenderer, "An §nadmin§r can run \"§i/offauth bind <user> <pubkey>§r\" to rebind your new key.", 15, 180 - this.textRenderer.fontHeight - 10, 0xFFFFFFFF, true);
        context.drawText(this.textRenderer, "You should provide them with your §onew§r public key.", 15, 190 - this.textRenderer.fontHeight - 10, 0xFFFFFFFF, true);
    }

    @Override
    public void close() {
        assert this.client != null;
        this.client.setScreen(parent);
    }
}
