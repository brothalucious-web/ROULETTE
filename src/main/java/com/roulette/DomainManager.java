package com.roulette;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Domain Expansion: Jackpot Palace.
 * Builds a temporary casino dome full of slot machines around the player, then puts every block back.
 */
public final class DomainManager {
	public static final String DOMAIN_NAME = "Jackpot Palace";
	public static final int RADIUS = 11;
	public static final int DURATION_TICKS = 20 * 60;
	public static final int COOLDOWN_TICKS = 20 * 180;
	/** How long the slot machine jackpot buffs last (4 minutes 11 seconds). */
	public static final int JACKPOT_BUFF_TICKS = 20 * 251;

	private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
	private static final String[] PROTECTED_BLOCKS = {"bedrock", "barrier", "end_portal", "end_portal_frame",
			"end_gateway", "reinforced_deepslate", "light", "structure_void", "nether_portal"};

	private static final List<Domain> ACTIVE = new ArrayList<>();
	private static final Map<UUID, Long> READY_AT = new HashMap<>();
	private static long now;

	private DomainManager() {
	}

	private static final class Domain {
		final ServerLevel level;
		final BlockPos center;
		final UUID owner;
		final Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
		final Set<BlockPos> machines = new HashSet<>();
		int ticksLeft = DURATION_TICKS;

		Domain(ServerLevel level, BlockPos center, UUID owner) {
			this.level = level;
			this.center = center;
			this.owner = owner;
		}
	}

	private static BlockState block(String id) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(id)).defaultBlockState();
	}

	public static void expand(ServerPlayer player) {
		if (player.isSpectator()) {
			return;
		}
		UUID id = player.getUUID();
		long readyAt = READY_AT.getOrDefault(id, 0L);
		if (now < readyAt) {
			player.sendSystemMessage(Prizes.prefix().append(Component.literal(
					"Your domain is recharging (" + ((readyAt - now) / 20 + 1) + "s left).").withStyle(ChatFormatting.GRAY)));
			return;
		}

		ServerLevel level = (ServerLevel) player.level();
		BlockPos center = player.blockPosition();
		int minGap = RADIUS * 2 + 2;
		for (Domain other : ACTIVE) {
			if (other.level == level
					&& Math.abs(other.center.getX() - center.getX()) < minGap
					&& Math.abs(other.center.getY() - center.getY()) < minGap
					&& Math.abs(other.center.getZ() - center.getZ()) < minGap) {
				player.sendSystemMessage(Prizes.prefix().append(Component.literal(
						"Too close to another domain.").withStyle(ChatFormatting.RED)));
				return;
			}
		}

		Domain domain = build(level, center, id);
		ACTIVE.add(domain);
		READY_AT.put(id, now + COOLDOWN_TICKS);

		level.playSound(null, center, SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace("block.end_portal.spawn")),
				SoundSource.PLAYERS, 1.0F, 1.3F);
		Component announcement = Component.literal("DOMAIN EXPANSION: ").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
				.append(Component.literal(DOMAIN_NAME).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
				.append(Component.literal("  (" + player.getName().getString() + ")").withStyle(ChatFormatting.GRAY));
		for (ServerPlayer nearby : level.players()) {
			if (isNear(nearby, center, 48)) {
				nearby.sendSystemMessage(announcement);
			}
		}
		player.sendSystemMessage(Component.literal("Right-click a slot machine to play. The domain lasts 60 seconds.")
				.withStyle(ChatFormatting.YELLOW));
	}

	private static Domain build(ServerLevel level, BlockPos center, UUID owner) {
		Set<Block> protectedBlocks = new HashSet<>();
		for (String name : PROTECTED_BLOCKS) {
			protectedBlocks.add(BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(name)));
		}
		BlockState air = block("air");
		BlockState black = block("black_concrete");
		BlockState red = block("red_concrete");
		BlockState gold = block("yellow_concrete");
		BlockState lantern = block("sea_lantern");
		BlockState glow = block("glowstone");

		Domain domain = new Domain(level, center, owner);
		int r = RADIUS;
		double outerSq = (r + 0.5) * (r + 0.5);
		double innerSq = (r - 1.0) * (r - 1.0);

		// Floor, dome shell, and an emptied interior.
		for (int dy = -1; dy <= r + 1; dy++) {
			for (int dx = -r - 1; dx <= r + 1; dx++) {
				for (int dz = -r - 1; dz <= r + 1; dz++) {
					int horizontalSq = dx * dx + dz * dz;
					BlockState target;
					if (dy == -1) {
						if (horizontalSq > outerSq) {
							continue;
						}
						int ring = (int) Math.round(Math.sqrt(horizontalSq));
						if (ring <= 1 || ring % 4 == 3) {
							target = gold;
						} else {
							target = ((dx + dz) & 1) == 0 ? red : black;
						}
					} else {
						int distSq = horizontalSq + dy * dy;
						if (distSq > outerSq) {
							continue;
						}
						if (distSq <= innerSq) {
							target = air;
						} else if (Math.floorMod(dx * 3 + dy * 5 + dz * 7, 9) == 0) {
							target = lantern;
						} else if (Math.floorMod(dy, 4) == 2) {
							target = gold;
						} else {
							target = black;
						}
					}
					place(domain, protectedBlocks, center.offset(dx, dy, dz), target);
				}
			}
		}

		// Two rings of slot machines: a gold cabinet with a glowing screen on top.
		addMachineRing(domain, protectedBlocks, 5, 8, gold, glow);
		addMachineRing(domain, protectedBlocks, 8, 14, gold, glow);
		return domain;
	}

	private static void addMachineRing(Domain domain, Set<Block> protectedBlocks, int radius, int count, BlockState body, BlockState screen) {
		for (int i = 0; i < count; i++) {
			double angle = Math.PI * 2.0 * i / count;
			int dx = (int) Math.round(Math.cos(angle) * radius);
			int dz = (int) Math.round(Math.sin(angle) * radius);
			BlockPos bottom = domain.center.offset(dx, 0, dz);
			BlockPos top = domain.center.offset(dx, 1, dz);
			if (place(domain, protectedBlocks, bottom, body) && place(domain, protectedBlocks, top, screen)) {
				domain.machines.add(bottom);
				domain.machines.add(top);
			}
		}
	}

	private static boolean place(Domain domain, Set<Block> protectedBlocks, BlockPos pos, BlockState target) {
		ServerLevel level = domain.level;
		if (!level.isInWorldBounds(pos)) {
			return false;
		}
		if (!domain.saved.containsKey(pos)) {
			// Never touch chests, furnaces, portals, bedrock and the like.
			if (level.getBlockEntity(pos) != null) {
				return false;
			}
			BlockState original = level.getBlockState(pos);
			if (protectedBlocks.contains(original.getBlock())) {
				return false;
			}
			domain.saved.put(pos.immutable(), original);
		}
		level.setBlock(pos, target, FLAGS);
		return true;
	}

	public static void tick() {
		now++;
		Iterator<Domain> iterator = ACTIVE.iterator();
		while (iterator.hasNext()) {
			Domain domain = iterator.next();
			domain.ticksLeft--;
			if (domain.ticksLeft == 200) {
				for (ServerPlayer player : domain.level.players()) {
					if (player.getUUID().equals(domain.owner)) {
						player.sendSystemMessage(Prizes.prefix().append(Component.literal(
								"Your domain collapses in 10 seconds!").withStyle(ChatFormatting.YELLOW)));
					}
				}
			}
			if (domain.ticksLeft <= 0) {
				iterator.remove();
				collapse(domain);
			}
		}
	}

	private static void collapse(Domain domain) {
		domain.machines.clear();
		List<Map.Entry<BlockPos, BlockState>> entries = new ArrayList<>(domain.saved.entrySet());
		for (int i = entries.size() - 1; i >= 0; i--) {
			Map.Entry<BlockPos, BlockState> entry = entries.get(i);
			domain.level.setBlock(entry.getKey(), entry.getValue(), FLAGS);
		}
		domain.saved.clear();

		// Anyone still inside goes back to the spot where the domain was cast, so the restored terrain can't trap them.
		BlockPos c = domain.center;
		for (ServerPlayer player : domain.level.players()) {
			if (isNear(player, c, RADIUS + 1)) {
				player.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
				player.sendSystemMessage(Prizes.prefix().append(Component.literal("The domain collapses.").withStyle(ChatFormatting.GRAY)));
			}
		}
	}

	public static void collapseAll() {
		for (Domain domain : new ArrayList<>(ACTIVE)) {
			collapse(domain);
		}
		ACTIVE.clear();
	}

	private static boolean isNear(ServerPlayer player, BlockPos pos, double range) {
		double dx = player.getX() - (pos.getX() + 0.5);
		double dy = player.getY() - pos.getY();
		double dz = player.getZ() - (pos.getZ() + 0.5);
		return dx * dx + dy * dy + dz * dz <= range * range;
	}

	public static boolean isMachine(Level level, BlockPos pos) {
		for (Domain domain : ACTIVE) {
			if (domain.level == level && domain.machines.contains(pos)) {
				return true;
			}
		}
		return false;
	}

	/** Domain blocks can't be broken, so nobody can mine their way out or keep the blocks. */
	public static boolean isDomainBlock(Level level, BlockPos pos) {
		for (Domain domain : ACTIVE) {
			if (domain.level == level && domain.saved.containsKey(pos)) {
				return true;
			}
		}
		return false;
	}
}
