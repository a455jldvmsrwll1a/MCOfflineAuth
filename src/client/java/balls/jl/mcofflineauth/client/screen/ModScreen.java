package balls.jl.mcofflineauth.client.screen;

import balls.jl.mcofflineauth.Constants;
import balls.jl.mcofflineauth.client.ClientKeyPair;
import balls.jl.mcofflineauth.util.KeyEncode;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.Clipboard;
import net.minecraft.text.Text;

public class ModScreen extends Screen {
    public final Screen parent;

    public ModScreen(Screen parent) {
        super(Text.empty());
        this.parent = parent;
    }

    @Override
    protected void init() {
        assert this.client != null;
        int width = this.client.getWindow().getScaledWidth();
        int height = this.client.getWindow().getScaledHeight();
        String keyString = KeyEncode.encodePublic(ClientKeyPair.KEY_PAIR.getPublic());
        ButtonWidget pkBtnWidget = ButtonWidget.builder(Text.of(keyString), (btn) -> {
            Clipboard clipboard = new Clipboard();
            clipboard.setClipboard(this.client.getWindow().getHandle(), keyString);
            this.client.getToastManager().add(SystemToast.create(this.client, SystemToast.Type.NARRATOR_TOGGLE, Text.of("Public key copied to clipboard!"), Text.of("You can share this key with others.")));
        }).tooltip(Tooltip.of(Text.literal("Copy to clipboard."))).dimensions(75, 150 - this.textRenderer.fontHeight - 15, width - 100 - 20, 20).build();

        ButtonWidget resetBtnWidget = ButtonWidget.builder(Text.of("§cRegenerate§r"), (btn) -> this.client.setScreen(new WipeConfirmationScreen(this))).tooltip(Tooltip.of(Text.literal("§lWipe§r the key-pair & make a new one."))).dimensions(15, height - this.textRenderer.fontHeight - 20, 80, 20).build();

        ButtonWidget reloadBtnWidget = ButtonWidget.builder(Text.of("Reload"), (btn) -> {
            try {
                ClientKeyPair.load();
            } catch (RuntimeException e) {
                this.client.getToastManager().add(SystemToast.create(this.client, SystemToast.Type.NARRATOR_TOGGLE, Text.of("Failed to load key-pair!"), Text.of(e.toString())));
            }
            this.client.setScreen(new ModScreen(this.parent));
        }).tooltip(Tooltip.of(Text.literal("Load the key from disk again. (e.g. if you changed the files while Minecraft was open.)"))).dimensions(15 + 80 + 10, height - this.textRenderer.fontHeight - 20, 80, 20).build();

        ButtonWidget explorerBtnWidget = ButtonWidget.builder(Text.of("Open Folder"), (btn) -> net.minecraft.util.Util.getOperatingSystem().open(Constants.MOD_DIR)).tooltip(Tooltip.of(Text.literal("Open the mod's directory with the native file manager."))).dimensions(15 + 80 + 80 + 10 + 10, height - this.textRenderer.fontHeight - 20, 100, 20).build();
        ButtonWidget cancelBtnWidget = ButtonWidget.builder(Text.of("Back"), (btn) -> close()).dimensions(width - 80 - 15, height - this.textRenderer.fontHeight - 20, 80, 20).build();
        ButtonWidget modPageWidget = ButtonWidget.builder(Text.of("Mod Page"), (btn) -> net.minecraft.util.Util.getOperatingSystem().open("https://github.com/a455jldvmsrwll1a/MCOfflineAuth?tab=readme-ov-file#usage-players")).tooltip(Tooltip.of(Text.literal("Visit mod page on Github."))).dimensions(15, 130 - this.textRenderer.fontHeight - 20, 80, 20).build();

        this.addDrawableChild(cancelBtnWidget);
        this.addDrawableChild(modPageWidget);
        this.addDrawableChild(pkBtnWidget);
        this.addDrawableChild(resetBtnWidget);
        this.addDrawableChild(reloadBtnWidget);
        this.addDrawableChild(explorerBtnWidget);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawText(this.textRenderer, "MC Offline Auth Configuration", 15, 40 - this.textRenderer.fontHeight - 10, 0xFFA719FF, true);
        context.drawText(this.textRenderer, "By jldmw1a.", 15, 60 - this.textRenderer.fontHeight - 10, 0xFF85F1FF, true);
        context.drawText(this.textRenderer, "Allows servers to have some form of authentication without using", 15, 80 - this.textRenderer.fontHeight - 10, 0xFFFFFFFF, true);
        context.drawText(this.textRenderer, "external authentication systems, lending itself to offline servers.", 15, 90 - this.textRenderer.fontHeight - 10, 0xFFFFFFFF, true);
        context.drawText(this.textRenderer, "THIS MOD IS EXPERIMENTAL SOFTWARE. See webpage below for usage guide.", 15, 105 - this.textRenderer.fontHeight - 10, 0xFFEB9B34, true);
        context.drawText(this.textRenderer, "Public key: ", 15, 150 - this.textRenderer.fontHeight - 10, 0xFFFFFFFF, true);
    }

    @Override
    public void close() {
        assert this.client != null;
        this.client.setScreen(parent);
    }
}
