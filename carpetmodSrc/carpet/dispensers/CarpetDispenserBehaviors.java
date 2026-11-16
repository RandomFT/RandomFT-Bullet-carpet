package carpet.dispensers;

import carpet.CarpetSettings;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * Behaviors de dispensers para reglas de Carpet.
 * Dinámico: consulta CarpetSettings.blazeMeal en cada tick de dispensado.
 */
public class CarpetDispenserBehaviors {

    /** Registra los behaviors una sola vez al arrancar el server. */
    public static void register() {
    //==== Coso para el dispenser con blaze powder
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(
                Items.BLAZE_POWDER,
                new BehaviorDefaultDispenseItem() {
                    @Override
                    protected @Nonnull ItemStack dispenseStack(IBlockSource source, ItemStack stack) {

                        if (!CarpetSettings.blazeMeal) {
                            return super.dispenseStack(source, stack);
                        }

                        World world = source.getWorld();
                        BlockPos frontPos = source.getBlockPos()
                                .offset(source.getBlockState().getValue(BlockDispenser.FACING));

                        IBlockState state = world.getBlockState(frontPos);

                        if (state.getBlock() == Blocks.NETHER_WART) {
                            int age = state.getValue(BlockNetherWart.AGE);
                            if (age < 3) {
                                if (!world.isRemote) {

                                    world.setBlockState(
                                            frontPos,
                                            state.withProperty(BlockNetherWart.AGE, age + 1),
                                            2
                                    );

                                    world.playEvent(2005, frontPos, 0);
                                    stack.shrink(1);
                                }
                                return stack;
                            } else {

                                if (!world.isRemote) {
                                    world.playEvent(2005, frontPos, -1);
                                }

                                return stack;
                            }
                        }


                        if (!world.isRemote) {
                            world.playEvent(2005, frontPos, -1);
                        }
                        return stack;
                    }
                }
        );
    }
}
