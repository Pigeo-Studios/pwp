package com.pigeostudios.pwp.warfare.block;

import com.pwp.api.security.PwpPermissions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Спавнер техники — «как барьер»: блок есть (таймеры, настройки, точка выезда), но его не видно, а
 * столкновение — как у барьера. Рамку, голограмму и клик видит только тот, кто его настраивает: креатив
 * или спавнер в руке (как у ванильного барьера). Остальным блок не выделяется и не кликается.
 */
public class VehicleSpawnerBlock extends BaseEntityBlock {
    public VehicleSpawnerBlock() {
        super(Properties.copy(Blocks.DROPPER)
                .strength(-1.0F, 3600000.0F)
                .noOcclusion()
                .isValidSpawn((s, l, p, e) -> false)
                .isSuffocating((s, l, p) -> false)
                .isViewBlocking((s, l, p) -> false));
    }

    /** Видит и настраивает спавнеры: креатив или спавнер техники в руке. Только показ, не право (право — в use). */
    public static boolean seesSpawners(@Nullable Player p) {
        if (p == null) return false;
        if (p.isCreative()) return true;
        var item = com.pigeostudios.pwp.warfare.item.ModItems.VEHICLE_SPAWNER_ITEM.get();
        return p.getMainHandItem().is(item) || p.getOffhandItem().is(item);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** Рамка выделения и попадание курсора — только тому, кто настраивает; остальным блока «нет». */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (ctx instanceof EntityCollisionContext ec && ec.getEntity() instanceof Player p && seesSpawners(p)) {
            return Shapes.block();
        }
        return Shapes.empty();
    }

    /** Столкновение — как у барьера: полный блок для всех (техника стоит на нём, как и раньше). */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.block();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VehicleSpawnerBlockEntity(pos, state);
    }

    // Открывает меню спаунера техники (только админ, op 2+)
    @Override
    public InteractionResult use(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!seesSpawners(player)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (PwpPermissions.isAdmin(player)) {
                if (level.getBlockEntity(pos) instanceof VehicleSpawnerBlockEntity spawner) {
                    NetworkHooks.openScreen((ServerPlayer) player, spawner, pos);
                }
            } else {
                player.displayClientMessage(Component.translatable("pwpwarfare.msg.access.admin_only"), true);
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(
                        type, (BlockEntityType) ModBlocks.VEHICLE_SPAWNER_BE.get(), VehicleSpawnerBlockEntity::tick);
    }
}
