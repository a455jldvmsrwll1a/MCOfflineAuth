package balls.jl.mcofflineauth.client.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ModScreen extends Screen {
    public final Screen parent;

    public ModScreen(Screen parent) {
        super(Text.empty());
        this.parent = parent;
    }


}
