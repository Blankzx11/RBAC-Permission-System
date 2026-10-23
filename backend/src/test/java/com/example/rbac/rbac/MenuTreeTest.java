package com.example.rbac.rbac;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MenuTreeTest {
    @Test
    void includesGrantedActionParentAsNonNavigableContainer() {
        List<PermissionRow> rows = List.of(
                new PermissionRow(10, null, "menu:system", "系统管理", "MENU", "/overview", 2),
                new PermissionRow(11, 10L, "menu:audit", "审计日志", "MENU", "/audit", 1),
                new PermissionRow(12, 11L, "sys:audit:read", "查看审计", "ACTION", null, 3),
                new PermissionRow(20, null, "menu:roles", "角色管理", "MENU", "/roles", 1));

        List<MenuItem> tree = PermissionService.buildMenuTree(
                rows, Set.of("sys:audit:read"), false);

        assertThat(tree).hasSize(1);
        MenuItem root = tree.getFirst();
        assertThat(root.id()).isEqualTo("10");
        assertThat(root.navigable()).isFalse();
        assertThat(root.children()).extracting(MenuItem::id).containsExactly("11");
        assertThat(root.children().getFirst().navigable()).isFalse();
    }

    @Test
    void keepsGrantedMenuNavigableAndSortsChildren() {
        List<PermissionRow> rows = List.of(
                new PermissionRow(1, null, "menu:root", "根菜单", "MENU", "/overview", 1),
                new PermissionRow(2, 1L, "menu:z", "Z", "MENU", "/roles", 20),
                new PermissionRow(3, 1L, "menu:a", "A", "MENU", "/audit", 10));

        List<MenuItem> tree = PermissionService.buildMenuTree(
                rows, Set.of("menu:root", "menu:z", "menu:a"), false);

        assertThat(tree.getFirst().navigable()).isTrue();
        assertThat(tree.getFirst().children()).extracting(MenuItem::id).containsExactly("3", "2");
        assertThat(tree.getFirst().children()).allMatch(MenuItem::navigable);
    }

    @Test
    void platformAdminSeesEveryMenu() {
        List<PermissionRow> rows = List.of(
                new PermissionRow(1, null, "menu:root", "根菜单", "MENU", "/overview", 1),
                new PermissionRow(2, 1L, "menu:audit", "审计", "MENU", "/audit", 2));

        List<MenuItem> tree = PermissionService.buildMenuTree(rows, Set.of(), true);

        assertThat(tree).hasSize(1);
        assertThat(tree.getFirst().navigable()).isTrue();
        assertThat(tree.getFirst().children().getFirst().navigable()).isTrue();
    }
}
