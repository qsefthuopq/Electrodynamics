package physica.forcefield.common.tile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.INpc;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidTank;
import physica.api.core.IGuiInterface;
import physica.api.core.ITileBase;
import physica.api.forcefield.IInvFortronTile;
import physica.forcefield.client.gui.GuiInterdictionMatrix;
import physica.forcefield.common.ForcefieldEventHandler;
import physica.forcefield.common.ForcefieldFluidRegister;
import physica.forcefield.common.ForcefieldItemRegister;
import physica.forcefield.common.effect.damage.DamageSourceForcefield;
import physica.forcefield.common.inventory.ContainerInterdictionMatrix;
import physica.forcefield.common.item.ItemFrequency;
import physica.forcefield.common.item.Permission;
import physica.library.location.BlockLocation;
import physica.library.tile.TileBaseContainer;

public class TileInterdictionMatrix extends TileBaseContainer implements IInvFortronTile, IGuiInterface {

	private Set<ITileBase> connections = new HashSet<>();
	private boolean isActivated;
	public static final int BASE_FORTRON = 1000;
	protected FluidTank fortronTank = new FluidTank(ForcefieldFluidRegister.LIQUID_FORTRON, 0, getMaxFortron());
	protected Set<ITileBase> fortronConnections = new HashSet<>();
	public static final int SLOT_FREQUENCY = 0;
	public static final int SLOT_STARTMODULEINDEX = 1;
	public static final int SLOT_ENDMODULEINDEX = 8;
	public static final int SLOT_STARTBANLIST = 9;
	public static final int SLOT_ENDBANLIST = 17;
	private int frequency;

	public int getMaxFortron() {
		return getFortronUse() * 3 + BASE_FORTRON;
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		ForcefieldEventHandler.INSTANCE.unregisterMatrix(this);
	}

	public int getFortronUse() {
		int extra = (Math.min(128, getModuleCount(ForcefieldItemRegister.moduleMap.get("moduleManipulationScale"), SLOT_STARTMODULEINDEX, SLOT_ENDMODULEINDEX)));
		return (int) (45000.91 * extra + (hasModule("moduleUpgradeAntiHostile") ? 100000 : 0)
				+ (hasModule("moduleUpgradeAntiFriendly") ? 100000 : 0)
				+ (hasModule("moduleUpgradeAntiPersonnel") ? 100000 : 0)
				+ (hasModule("moduleUpgradeAntiSpawn") ? 75000 : 0)
				+ (hasModule("moduleUpgradeBlockAccess") ? 75000 : 0)
				+ (hasModule("moduleUpgradeBlockAlter") ? 75000 : 0)
				+ (hasModule("moduleUpgradeConfiscate") ? 50000 : 0));
	}

	@Override
	public int getFrequency() {
		return frequency;
	}

	@Override
	public void setFrequency(int freq) {
		int oldFrequency = frequency;
		frequency = freq;
		if (oldFrequency != freq) {
			onFirstUpdate();
		}
	}

	@Override
	public void onFirstUpdate() {
		invalidateConnections();
		fortronConnections.clear();
		findNearbyConnections(TileFortronCapacitor.class);
	}

	@Override
	public void updateCommon(int ticks) {
		super.updateCommon(ticks);
		fortronTank.setCapacity(getMaxFortron());
		if (fortronTank.getCapacity() < fortronTank.getFluidAmount()) {
			fortronTank.getFluid().amount = fortronTank.getCapacity();
		}
		if (getStackInSlot(SLOT_FREQUENCY) != null) {
			if (getStackInSlot(SLOT_FREQUENCY).stackTagCompound != null) {
				setFrequency(getStackInSlot(SLOT_FREQUENCY).stackTagCompound.getInteger("frequency"));
			}
		}
	}

	public boolean isPlayerValidated(EntityPlayer player, Permission perm) {
		if (player.capabilities.isCreativeMode || player.isEntityInvulnerable()) {
			return true;
		}
		for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
			BlockLocation loc = new BlockLocation(xCoord + direction.offsetX, yCoord + direction.offsetY, zCoord + direction.offsetZ);
			TileEntity tile = loc.getTile(worldObj);
			if (tile instanceof TileBiometricIdentifier) {
				TileBiometricIdentifier identifier = (TileBiometricIdentifier) tile;
				if (identifier.isPlayerValidated(player, perm)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isValidatedItem(ItemStack stack) {
		for (int index = SLOT_STARTBANLIST; index <= SLOT_ENDBANLIST; index++) {
			ItemStack stack2 = getStackInSlot(index);
			if (stack2 == null) {
				continue;
			}
			if (stack == null || stack2.getItem() != stack.getItem()) {
				return false;
			}
		}
		return true;
	}

	public boolean isBlackList = true;

	public Set<ItemStack> getBlacklistedItems() {
		HashSet<ItemStack> set = new HashSet<>();
		if (isBlackList) {
			for (int index = SLOT_STARTBANLIST; index <= SLOT_ENDBANLIST; index++) {
				ItemStack stack = getStackInSlot(index);
				if (stack != null) {
					set.add(stack);
				}
			}
		}
		return set;
	}

	@Override
	public void updateServer(int ticks) {
		super.updateServer(ticks);
		if (!ForcefieldEventHandler.INSTANCE.isMatrixRegistered(this)) {
			ForcefieldEventHandler.INSTANCE.registerMatrix(this);
		}
		if (ticks % 20 == 0) {
			validateConnections();
		}
		isActivated = isPoweredByRedstone();

		if (isActivated() && fortronTank.getFluidAmount() > getFortronUse()) {
			fortronTank.drain(getFortronUse(), true);
			@SuppressWarnings("unchecked")
			List<EntityLivingBase> activeBBEntities = worldObj.getEntitiesWithinAABB(EntityLivingBase.class, getActiveBB());
			boolean confiscate = hasModule("moduleUpgradeConfiscate"), antiHostile = hasModule("moduleUpgradeAntiHostile"), antiFriendly = hasModule("moduleUpgradeAntiFriendly"),
					antiPersonnel = hasModule("moduleUpgradeAntiPersonnel");
			if (ticks % 3 == 0) {
				for (EntityLivingBase living : activeBBEntities) {
					if (antiFriendly) {
						onDefendAntiFriendly(living);
					}
					if (antiHostile) {
						onDefendAntiHostile(living);
					}
					if (antiPersonnel || confiscate) {
						onDefendAntiPersonnel(living, confiscate);
					}
				}
			}
			if (ticks % 30 == 0) {
				@SuppressWarnings("unchecked")
				List<Object> warnBBEntities = worldObj.getEntitiesWithinAABB(EntityLivingBase.class, getActiveWarnBB());
				for (Object obj : warnBBEntities) {
					if (obj instanceof EntityPlayer) {
						EntityPlayer player = (EntityPlayer) obj;
						if (!isPlayerValidated(player, null)) {
							player.addChatMessage(new ChatComponentText("Warning! Leave this zone immediately. You have no right to enter."));
						}
					}
				}
			}
		}
	}

	public void onDefendAntiFriendly(EntityLivingBase living) {
		if ((!(living instanceof IMob) || living instanceof INpc) && !(living instanceof EntityPlayer)) {
			living.attackEntityFrom(DamageSource.magic, 20);
		}
	}

	public void onDefendAntiHostile(EntityLivingBase living) {
		if (living instanceof IMob && !(living instanceof INpc) && !(living instanceof EntityPlayer)) {
			living.attackEntityFrom(DamageSource.magic, 20);
		}
	}

	public void onDefendAntiPersonnel(EntityLivingBase living, boolean confiscate) {
		if (living instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) living;
			if (!isPlayerValidated(player, Permission.BYPASS_INTERDICTION_MATRIX)) {
				player.addChatMessage(new ChatComponentText("Warning! Leave this zone immediately. You are in the scan zone!"));
				if (confiscate && !isPlayerValidated(player, Permission.BYPASS_CONFISCATION)) {
					Set<ItemStack> list_items = getBlacklistedItems();
					for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++) {
						ItemStack stack = player.inventory.getStackInSlot(slot);
						if (stack == null) {
							continue;
						}
						if (list_items.isEmpty()) {
							if (mergeIntoInventory(this, stack)) {
								player.inventory.setInventorySlotContents(slot, null);
							}
							continue;
						}
						for (ItemStack blacklisted : list_items) {
							if (isBlackList) {
								if (blacklisted.isItemEqual(stack)) {
									if (mergeIntoInventory(this, stack)) {
										player.inventory.setInventorySlotContents(slot, null);
									}
								}
							} else {
								if (blacklisted.isItemEqual(stack)) {
									break;
								}
							}
						}
					}
				}
				if (hasModule("moduleUpgradeAntiPersonnel")) {
					living.attackEntityFrom(DamageSourceForcefield.INSTANCE, 20);
				}
			}
		}
	}

	public static boolean mergeIntoInventory(TileEntity from, ItemStack stack) {
		for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
			stack = placeAdjInv(from, stack, dir);
			if (stack == null || stack.stackSize <= 0) {
				return true;
			}
		}
		return false;
	}

	private static ItemStack placeAdjInv(TileEntity tile, ItemStack stack, ForgeDirection dir) {
		TileEntity tileEntity = tile.getWorldObj().getTileEntity(tile.xCoord + dir.offsetX, tile.yCoord + dir.offsetY, tile.zCoord + dir.offsetZ);
		if (stack != null && tileEntity != null) {
			if (tileEntity instanceof TileEntityChest) {
				TileEntityChest chest1 = (TileEntityChest) tileEntity;
				return addToInv_first(chest1, stack);
			} else if (tileEntity instanceof ISidedInventory) {
				ISidedInventory inv = (ISidedInventory) tileEntity;
				int[] slot = inv.getAccessibleSlotsFromSide(dir.ordinal());
				for (int s : slot) {
					if (inv.canInsertItem(s, stack, dir.ordinal())) {
						stack = addToInv_slot(inv, stack, s);
						if (stack == null || stack.stackSize <= 0) {
							return null;
						}
					}
				}
			} else if (tileEntity instanceof IInventory) {
				IInventory inv = (IInventory) tileEntity;
				return addToInv_first(inv, stack);
			}
		}
		return stack;
	}

	private static ItemStack addToInv_first(IInventory inv, ItemStack stack) {
		if (stack == null || stack.stackSize <= 0) {
			return null;
		}
		for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
			ItemStack item = addToInv_slot(inv, stack, slot);
			if (item == null || item.stackSize <= 0) {
				return null;
			}
		}
		return stack;
	}

	private static ItemStack addToInv_slot(IInventory inv, ItemStack stack, int slot) {
		if (stack == null || stack.stackSize <= 0) {
			return null;
		}
		ItemStack item = inv.getStackInSlot(slot);
		if (item == null) {
			inv.setInventorySlotContents(slot, stack);
			return null;
		} else if (add_stack(item, stack, Math.min(inv.getInventoryStackLimit(), item.getMaxStackSize())) && stack.stackSize <= 0) {
			return null;
		}
		return stack;
	}

	private static boolean add_stack(ItemStack item, ItemStack stack, int maxStackSize) {
		if (item != null && item.isItemEqual(stack) && item.isStackable()) {
			int newSize = Math.min(item.stackSize + stack.stackSize, maxStackSize);
			stack.stackSize -= newSize - item.stackSize;
			item.stackSize = newSize;
			return true;
		}
		return false;
	}

	public boolean hasModule(String name) {
		return findModule(ForcefieldItemRegister.moduleMap.get(name), SLOT_STARTMODULEINDEX, SLOT_ENDMODULEINDEX);
	}

	public AxisAlignedBB getActiveBB() {
		int scaleCount = Math.min(128, getModuleCount(ForcefieldItemRegister.moduleMap.get("moduleManipulationScale"), SLOT_STARTMODULEINDEX, SLOT_ENDMODULEINDEX));
		return AxisAlignedBB.getBoundingBox(xCoord - scaleCount, yCoord - scaleCount, zCoord - scaleCount, xCoord + scaleCount + 1, yCoord + scaleCount + 1, zCoord + scaleCount + 1);
	}

	public AxisAlignedBB getActiveWarnBB() {
		int scaleCount = Math.min(128, getModuleCount(ForcefieldItemRegister.moduleMap.get("moduleManipulationScale"), SLOT_STARTMODULEINDEX, SLOT_ENDMODULEINDEX)) * 2;
		return AxisAlignedBB.getBoundingBox(xCoord - scaleCount, yCoord - scaleCount, zCoord - scaleCount, xCoord + scaleCount + 1, yCoord + scaleCount + 1, zCoord + scaleCount + 1);
	}

	@Override
	public void writeSynchronizationPacket(List<Object> dataList, EntityPlayer player) {
		dataList.add(isActivated);
		dataList.add(frequency);
		dataList.add(fortronTank.writeToNBT(new NBTTagCompound()));
		super.writeSynchronizationPacket(dataList, player);
	}

	@Override
	public void readSynchronizationPacket(ByteBuf buf, EntityPlayer player) {
		isActivated = buf.readBoolean();
		frequency = buf.readInt();
		fortronTank.readFromNBT(ByteBufUtils.readTag(buf));
		super.readSynchronizationPacket(buf, player);
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		tag.setInteger("frequency", frequency);;
		super.writeToNBT(tag);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		frequency = tag.getInteger("frequency");
		super.readFromNBT(tag);
	}

	@Override
	public boolean isActivated() {
		return isActivated;
	}

	@Override
	public Set<ITileBase> getFortronConnections() {
		return connections;
	}

	@Override
	public FluidTank getFortronTank() {
		return fortronTank;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		return ACCESSIBLE_SLOTS_NONE;
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, int side) {
		return false;
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int side) {
		return false;
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		if (slot == 0) {
			return stack != null && stack.getItem() instanceof ItemFrequency;
		} else if (slot <= SLOT_ENDMODULEINDEX) {
			return stack != null && (stack.getItem() == ForcefieldItemRegister.itemMetaUpgradeModule || stack.getItem() == ForcefieldItemRegister.itemMetaManipulationModule);
		} else {
			return true;
		}
	}

	@Override
	public int getSizeInventory() {
		return 18;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getClientGuiElement(int id, EntityPlayer player) {
		return new GuiInterdictionMatrix(player, this);
	}

	@Override
	public Container getServerGuiElement(int id, EntityPlayer player) {
		return new ContainerInterdictionMatrix(player, this);
	}

	public int getWarnRange() {
		return Math.min(128, getActionRange()) * 2;
	}

	public int getActionRange() {
		return Math.min(128, getModuleCount(ForcefieldItemRegister.moduleMap.get("moduleManipulationScale"), SLOT_STARTMODULEINDEX, SLOT_ENDMODULEINDEX));
	}

}
