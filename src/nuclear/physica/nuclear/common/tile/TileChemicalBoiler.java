package physica.nuclear.common.tile;

import java.util.List;

import cofh.api.energy.IEnergyContainerItem;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import physica.api.core.IGuiInterface;
import physica.library.tile.TileBasePoweredContainer;
import physica.nuclear.client.gui.GuiChemicalBoiler;
import physica.nuclear.common.NuclearFluidRegister;
import physica.nuclear.common.inventory.ContainerChemicalBoiler;
import physica.nuclear.common.recipe.NuclearCustomRecipeHelper;
import physica.nuclear.common.recipe.type.ChemicalBoilerRecipe;

public class TileChemicalBoiler extends TileBasePoweredContainer implements IGuiInterface, IFluidHandler {

	public static final int TICKS_REQUIRED = 800;
	public static final int SLOT_ENERGY = 0;
	public static final int SLOT_INPUT1 = 1;
	public static final int SLOT_INPUT2 = 2;

	private static final int[] ACCESSIBLE_SLOTS_UP = new int[] { SLOT_INPUT1, SLOT_INPUT2 };
	private static final int[] ACCESSIBLE_SLOTS_DOWN = new int[] { SLOT_INPUT1 };

	protected FluidTank waterTank = new FluidTank(FluidRegistry.WATER, 0, 5000);
	protected FluidTank hexaTank = new FluidTank(NuclearFluidRegister.LIQUID_HE, 0, 5000);

	protected int operatingTicks = 0;

	@Override
	public void updateServer(int ticks) {
		super.updateServer(ticks);
		if (hasEnoughEnergy()) {
			if (canProcess()) {
				if (operatingTicks < TICKS_REQUIRED) {
					operatingTicks++;
					ItemStack input = getStackInSlot(SLOT_INPUT2);
					ChemicalBoilerRecipe recipe = NuclearCustomRecipeHelper.getBoilerRecipe(input.getItem());
					waterTank.drain(recipe.getWaterUse() / TICKS_REQUIRED, true);
				} else {
					process();
					operatingTicks = 0;
				}
				extractEnergy();
			} else {
				operatingTicks = 0;
			}
		} else {
			drainBattery(SLOT_ENERGY);
			operatingTicks = 0;
		}
		ItemStack waterInput = getStackInSlot(SLOT_INPUT1);
		if (waterInput != null && waterInput.getItem() == Items.water_bucket && waterTank.getFluidAmount() < waterTank.getCapacity()) {
			setInventorySlotContents(SLOT_INPUT1, new ItemStack(Items.bucket));
			waterTank.fill(new FluidStack(FluidRegistry.WATER, 1000), true);
		}
	}

	private boolean canProcess() {
		ItemStack input = getStackInSlot(SLOT_INPUT2);
		if (input != null) {
			if (NuclearCustomRecipeHelper.isBoilerInput(input.getItem())) {
				ChemicalBoilerRecipe recipe = NuclearCustomRecipeHelper.getBoilerRecipe(input.getItem());
				if (recipe.getWaterUse() <= waterTank.getFluidAmount()) {
					return hexaTank.getFluidAmount() < hexaTank.getCapacity();
				}
			}
		}
		return false;
	}

	private void process() {
		ItemStack input = getStackInSlot(SLOT_INPUT2);
		ChemicalBoilerRecipe recipe = NuclearCustomRecipeHelper.getBoilerRecipe(input.getItem());
		hexaTank.fill(new FluidStack(NuclearFluidRegister.LIQUID_HE, recipe.getHexafluorideGenerated()), true);
		decrStackSize(SLOT_INPUT2, 1);

	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		NBTTagCompound waterCompound = new NBTTagCompound();
		waterTank.writeToNBT(waterCompound);
		NBTTagCompound hexaCompound = new NBTTagCompound();
		hexaTank.writeToNBT(hexaCompound);
		nbt.setTag("WaterTank", waterCompound);
		nbt.setTag("HexaTank", hexaCompound);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		NBTTagCompound waterCompound = (NBTTagCompound) nbt.getTag("WaterTank");
		NBTTagCompound hexaCompound = (NBTTagCompound) nbt.getTag("HexaTank");
		waterTank.readFromNBT(waterCompound);
		hexaTank.readFromNBT(hexaCompound);
	}

	@Override
	public void writeClientGuiPacket(List<Object> dataList, EntityPlayer player) {
		super.writeClientGuiPacket(dataList, player);
		dataList.add(operatingTicks);
		dataList.add(waterTank.writeToNBT(new NBTTagCompound()));
		dataList.add(hexaTank.writeToNBT(new NBTTagCompound()));
	}

	@Override
	public void readClientGuiPacket(ByteBuf buf, EntityPlayer player) {
		super.readClientGuiPacket(buf, player);
		operatingTicks = buf.readInt();
		waterTank.readFromNBT(ByteBufUtils.readTag(buf));
		hexaTank.readFromNBT(ByteBufUtils.readTag(buf));
	}

	public int getOperatingTicks() {
		return operatingTicks;
	}

	public FluidTank getWaterTank() {
		return waterTank;
	}

	public FluidTank getHexTank() {
		return hexaTank;
	}

	@Override
	public int getSizeInventory() {
		return 3;
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		return stack != null && (slot == SLOT_ENERGY ? stack.getItem() instanceof IEnergyContainerItem
				: slot == SLOT_INPUT2 ? NuclearCustomRecipeHelper.isBoilerInput(stack.getItem())
						: slot == SLOT_INPUT1 && stack.getItem() == Items.water_bucket);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getClientGuiElement(int id, EntityPlayer player) {
		return new GuiChemicalBoiler(player, this);
	}

	@Override
	public Container getServerGuiElement(int id, EntityPlayer player) {
		return new ContainerChemicalBoiler(player, this);
	}

	@Override
	public int getMaxEnergyStored(ForgeDirection from) {
		return getEnergyUsage() * 2;
	}

	@Override
	public boolean canConnectEnergy(ForgeDirection from) {
		return true;
	}

	@Override
	public int getEnergyUsage() {
		return 400;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		return side == ForgeDirection.UP.ordinal() ? ACCESSIBLE_SLOTS_UP : side == ForgeDirection.DOWN.ordinal() ? ACCESSIBLE_SLOTS_DOWN : ACCESSIBLE_SLOTS_NONE;
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, int side) {
		return isItemValidForSlot(slot, stack);
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int side) {
		return slot == SLOT_INPUT1 && getStackInSlot(SLOT_INPUT1) != null && getStackInSlot(SLOT_INPUT1).getItem() == Items.bucket;
	}

	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
		return resource != null && resource.getFluid() == FluidRegistry.WATER ? waterTank.fill(resource, doFill)
				: resource != null && resource.getFluid() == NuclearFluidRegister.LIQUID_HE ? hexaTank.fill(resource, doFill) : 0;
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
		return null;
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
		return hexaTank.drain(maxDrain, doDrain);
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid) {
		return fluid == FluidRegistry.WATER || fluid == NuclearFluidRegister.LIQUID_HE;
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid) {
		return fluid == NuclearFluidRegister.LIQUID_HE;
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from) {
		return new FluidTankInfo[] { waterTank.getInfo(), hexaTank.getInfo() };
	}

	public boolean isRotating() {
		return getOperatingTicks() > 0;
	}
}
