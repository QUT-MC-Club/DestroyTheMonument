package eu.pb4.destroythemonument.ui;

import eu.pb4.destroythemonument.game.logic.BaseGameLogic;
import eu.pb4.destroythemonument.game.data.PlayerData;
import eu.pb4.destroythemonument.game.playerclass.PlayerClass;
import eu.pb4.destroythemonument.game.playerclass.ClassRegistry;
import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.destroythemonument.other.FormattingUtil;
import eu.pb4.sgui.api.GuiHelpers;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.GuiInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

import java.util.ArrayList;
import java.util.List;

public class ClassSelectorUI extends SimpleGui {
    private final PlayerData playerData;
    private final BaseGameLogic game;
    private final List<PlayerClass> kits;
    @Nullable
    private final GuiInterface previousUi;

    public ClassSelectorUI(ServerPlayerEntity player, PlayerData data, BaseGameLogic game, List<PlayerClass> kits) {
        super(getType(kits.size()), player, kits.size() > 53);
        this.playerData = data;
        this.game = game;
        this.kits = kits;
        this.previousUi = GuiHelpers.getCurrentGui(player);
        this.setTitle(DtmUtil.getText("ui", "select_class"));
        this.updateIcons();
    }

    private static ScreenHandlerType<?> getType(int size) {
        if (size <= 9) {
            return ScreenHandlerType.GENERIC_9X1;
        } else if (size <= 18) {
            return ScreenHandlerType.GENERIC_9X2;
        } else if (size <= 27) {
            return ScreenHandlerType.GENERIC_9X3;
        } else if (size <= 36) {
            return ScreenHandlerType.GENERIC_9X4;
        } else if (size <= 45) {
            return ScreenHandlerType.GENERIC_9X5;
        } else {
            return ScreenHandlerType.GENERIC_9X6;
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        if (this.previousUi != null) {
            this.previousUi.open();
        }
    }

    public static void openSelector(ServerPlayerEntity player, BaseGameLogic logic) {
        new ClassSelectorUI(player, logic.participants.get(PlayerRef.of(player)), logic, logic.kits).open();
    }

    public static void openSelector(ServerPlayerEntity player, PlayerData data, List<Identifier> kits) {
        ArrayList<PlayerClass> kitsList = new ArrayList<>();

        for (Identifier id : kits) {
            PlayerClass kit = ClassRegistry.get(id);
            if (kit != null) {
                kitsList.add(kit);
            }
        }

        new ClassSelectorUI(player, data, null, kitsList).open();
    }

    public void updateIcons() {
        int pos = 0;

        for (PlayerClass kit : this.kits) {
            GuiElementBuilder icon = GuiElementBuilder.from(kit.icon());
            icon.setName(DtmUtil.getText("class", kit.name()));
            icon.hideDefaultTooltip();
            if (kit == this.playerData.selectedClass) {
                icon.glow();
            }
            icon.addLoreLine(DtmUtil.getText("class", kit.name() + "/description").formatted(Formatting.RED));
            icon.addLoreLine(Text.empty());
            icon.addLoreLine(FormattingUtil.format(FormattingUtil.GENERAL_PREFIX, DtmUtil.getText("ui", "click_select").formatted(Formatting.GRAY)));
            icon.addLoreLine(FormattingUtil.format(FormattingUtil.GENERAL_PREFIX, DtmUtil.getText("ui", "click_preview").formatted(Formatting.GRAY)));

            icon.setCallback((x, clickType, z) -> {
                if (clickType.isLeft) {
                    this.player.playSoundToPlayer(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.MASTER, 0.5f, 1);
                    changeKit(this.game, this.player, this.playerData, kit);
                } else if (clickType.isRight) {
                    this.player.playSoundToPlayer(SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.MASTER, 0.5f, 1);
                    new ClassPreviewUI(this, kit).open();
                }
                this.updateIcons();
            });

            this.setSlot(pos, icon);
            pos++;
        }
    }

    public static void changeKit(BaseGameLogic game, ServerPlayerEntity player, PlayerData playerData, PlayerClass kit) {
        playerData.selectedClass = kit;

        MutableText text = FormattingUtil.format(FormattingUtil.GENERAL_PREFIX, FormattingUtil.GENERAL_STYLE, DtmUtil.getText("message", "selected_class",
                DtmUtil.getText("class", kit.name()).formatted(Formatting.GOLD)));

        player.sendMessage(text, false);
        boolean isIn = false;
        if (game != null) {
            for (BlockBounds classChange : playerData.teamData.classChange) {
                if (classChange.contains(player.getBlockPos())) {
                    isIn = true;
                    break;
                }
            }

            if (isIn && !game.deadPlayers.containsKey(PlayerRef.of(player))) {
                playerData.activeClass = kit;
                playerData.resetTimers();
                game.setupPlayerClass(player, playerData);
            } else {
                player.sendMessage(FormattingUtil.format(FormattingUtil.GENERAL_PREFIX, FormattingUtil.GENERAL_STYLE, DtmUtil.getText("message", "class_respawn")), false);
            }
        }
    }
}
