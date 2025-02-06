package balls.jl.mcofflineauth.client.modmenu;

import balls.jl.mcofflineauth.client.screen.ModScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuApiImpl implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModScreen::new;
    }
}

