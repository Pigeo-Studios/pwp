package com.pigeostudios.pwp.medicine;

import com.pigeostudios.pwp.medicine.client.ModClientSetup;
import com.pigeostudios.pwp.medicine.config.PWPConfig;
import com.pigeostudios.pwp.medicine.effect.ModEffects;
import com.pigeostudios.pwp.medicine.item.ModItems;
import com.pigeostudios.pwp.medicine.sound.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

// Главный класс мода PWP Medicine.
// Инициализирует предметы, звуки, эффекты, креативную вкладку и конфиг.
@Mod(value="pwp_medicine")
public class PWPMedicine {
    public static final String MODID = "pwp_medicine";
    // Регистратор креативной вкладки с предметами мода
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(ResourceKey.createRegistryKey(new ResourceLocation("minecraft", "creative_mode_tab")), "pwp_medicine");
    // Вкладка "Медицина" с иконкой аптечки
    public static final RegistryObject<CreativeModeTab> MEDICINE_TAB = CREATIVE_MODE_TABS.register("medicine_tab", () -> CreativeModeTab.builder().icon(() -> new ItemStack((ItemLike)ModItems.MEDKIT.get())).title(Component.translatable("creativetab.medicine_tab")).displayItems((parameters, output) -> {
        output.accept((ItemLike)ModItems.BANDAGE.get());
        output.accept((ItemLike)ModItems.MEDKIT.get());
    }).build());

    // Конструктор — регистрирует всё на шине событий
    public PWPMedicine() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        // Регистрация конфига
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, (IConfigSpec)PWPConfig.SPEC);
        // Экран настроек для клиента
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModClientSetup.registerConfigScreen();
        }
        // Регистрация всех подсистем
        ModItems.register(modEventBus);
        ModSounds.register(modEventBus);
        ModEffects.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
