package com.mcjty.container;

import com.mcjty.rftools.RFTools;
import com.mcjty.rftools.blocks.BlockTools;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public abstract class GenericContainerBlock extends BlockContainer {

    private IIcon iconFront;
    private IIcon iconSide;
    private Class<? extends TileEntity> tileEntityClass;

    protected GenericContainerBlock(Material material, Class<? extends TileEntity> tileEntityClass) {
        super(material);
        this.tileEntityClass = tileEntityClass;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float sx, float sy, float sz) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (!tileEntityClass.isInstance(te)) {
            return true;
        }
        if (world.isRemote) {
            return true;
        }
        player.openGui(RFTools.instance, getGuiID(), world, x, y, z);
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int i) {
        return null;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        try {
            return tileEntityClass.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void registerBlockIcons(IIconRegister iconRegister) {
        iconFront = iconRegister.registerIcon(RFTools.MODID + ":" + getFrontIconName());
        iconSide = iconRegister.registerIcon(RFTools.MODID + ":" + "machineSide");
    }

    /**
     * Return the name of the icon to be used for the front side of the machine.
     * @return
     */
    public abstract String getFrontIconName();

    /**
     * Return the id of the gui to use for this block.
     * @return
     */
    public abstract int getGuiID();

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entityLivingBase, ItemStack itemStack) {
        ForgeDirection dir = BlockTools.determineOrientation(x, y, z, entityLivingBase);
        int meta = world.getBlockMetadata(x, y, z);
        world.setBlockMetadataWithNotify(x, y, z, BlockTools.setOrientation(meta, dir), 2);
    }

    @Override
    public IIcon getIcon(IBlockAccess blockAccess, int x, int y, int z, int side) {
        int meta = blockAccess.getBlockMetadata(x, y, z);
        ForgeDirection k = BlockTools.getOrientation(meta);
        if (side == k.ordinal()) {
            return iconFront;
        } else {
            return iconSide;
        }
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        if (side == 3) {
            return iconFront;
        } else {
            return iconSide;
        }
    }
}