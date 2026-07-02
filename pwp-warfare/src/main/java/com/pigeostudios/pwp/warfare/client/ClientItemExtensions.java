package com.pigeostudios.pwp.warfare.client;

import com.pigeostudios.pwp.warfare.client.renderer.EntrenchingToolRenderer;
import com.pigeostudios.pwp.warfare.client.renderer.SquadRadioRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

// Расширения клиентских предметов для кастомного рендера
// Сапёрная лопатка и радиостанция имеют собственные 3D-модели
public class ClientItemExtensions {
   public static final IClientItemExtensions ENTRENCHING_TOOL = new IClientItemExtensions() {
      private EntrenchingToolRenderer renderer;

      public BlockEntityWithoutLevelRenderer getCustomRenderer() {
         if (this.renderer == null) {
            this.renderer = new EntrenchingToolRenderer();
         }

         return this.renderer;
      }
   };
   public static final IClientItemExtensions RALLY_RADIO = new IClientItemExtensions() {
      private SquadRadioRenderer renderer;

      public BlockEntityWithoutLevelRenderer getCustomRenderer() {
         if (this.renderer == null) {
            this.renderer = new SquadRadioRenderer();
         }

         return this.renderer;
      }
   };
}
