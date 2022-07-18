package net.lextyi.elytrahelper.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin
{
	public MinecraftClient client = MinecraftClient.getInstance();
	public boolean flight = false;

	@Inject(at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/client/network/ClientPlayerEntity;getEquippedStack(Lnet/minecraft/entity/EquipmentSlot;)Lnet/minecraft/item/ItemStack;"),
			method = "tickMovement")
	private void beforeTakeoff(CallbackInfo info)
	{
		if (client.player != null)
		{
			if (!client.player.isOnGround() && !client.player.isFallFlying() && !client.player.hasStatusEffect(StatusEffects.LEVITATION))
			{
				for (int slot = 0; slot < 36; slot++)
				{
					ItemStack stack = client.player.getInventory().getStack(slot);

					if (stack.isOf(Items.ELYTRA) && ElytraItem.isUsable(stack) && !EnchantmentHelper.hasBindingCurse(stack))
					{
						if (client.interactionManager != null)
						{
							client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, 6, slot, SlotActionType.SWAP, client.player);
						}

						client.player.networkHandler.sendPacket(new ClientCommandC2SPacket(client.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
						flight = true;
						break;
					}
				}
			}
		}
	}

	@Inject(at = @At(value = "HEAD"), method = "tickMovement")
	private void afterTakeoff(CallbackInfo info)
	{
		if (client.player != null)
		{
			if (client.player.isFallFlying())
			{
				flight = true;
				controlSpeed();
				controlHeight();

				if (client.player.getInventory().getArmorStack(2).getDamage() >= 430)
				{
					for (int slot = 0; slot < 36; slot++)
					{
						ItemStack stack = client.player.getInventory().getStack(slot);

						if (stack.isOf(Items.ELYTRA) && stack.getDamage() < 430 && !EnchantmentHelper.hasBindingCurse(stack))
						{
							if (client.interactionManager != null)
							{
								client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, 6, slot, SlotActionType.SWAP, client.player);
							}

							break;
						}
					}
				}
			}
			else if (flight && client.player.isOnGround())
			{
				flight = false;

				if (!(client.player.getInventory().getArmorStack(2).getItem() instanceof ArmorItem))
				{
					for (int slot = 0; slot < 36; slot++)
					{
						ItemStack stack = client.player.getInventory().getStack(slot);

						if (stack.getItem() instanceof ArmorItem && !EnchantmentHelper.hasBindingCurse(stack))
						{
							if (client.interactionManager != null)
							{
								client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, 6, slot, SlotActionType.SWAP, client.player);
							}

							break;
						}
					}
				}
			}
		}
	}

	private void controlSpeed()
	{
		if (client.player != null)
		{
			float yaw = (float)Math.toRadians(client.player.getYaw());
			Vec3d forward = new Vec3d(-MathHelper.sin(yaw) * 0.05, 0, MathHelper.cos(yaw) * 0.05), v = client.player.getVelocity();

			if (client.options.keyForward.isPressed())
			{
				client.player.setVelocity(v.add(forward));
			}
			else if (client.options.keyBack.isPressed())
			{
				client.player.setVelocity(v.subtract(forward));
			}
		}
	}

	private void controlHeight()
	{
		if (client.player != null)
		{
			Vec3d v = client.player.getVelocity();

			if (client.options.keyJump.isPressed())
			{
				client.player.setVelocity(v.x, v.y + 0.08, v.z);
			}
			else if (client.options.keySneak.isPressed())
			{
				client.player.setVelocity(v.x, v.y - 0.04, v.z);
			}
		}
	}
}
