"use client";

import { Menu } from "@base-ui/react/menu";
import { useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useId, useState } from "react";
import { signOutFromGoogle } from "../lib/firebase-auth";
import { GAME_LINKS, MANAGER_LINKS } from "../lib/nav-links";
import { endSession } from "../lib/session-client";
import { AUTH_ME_KEY, useAuth } from "./auth-provider";
import { Button } from "./ui/button";

const linkClass =
  "rounded-md px-3 py-2 text-sm font-medium text-primary-fg focus-visible:outline-2 focus-visible:outline-offset-2";

const menuItemClass =
  "block px-3 py-2 text-sm text-text outline-hidden focus-visible:outline-2 focus-visible:outline-solid focus-visible:outline-current focus-visible:outline-offset-2 data-highlighted:bg-primary data-highlighted:text-primary-fg";

export function Navbar() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { isManager } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const [adminOpen, setAdminOpen] = useState(false);
  const [logoutPending, setLogoutPending] = useState(false);
  const [logoutError, setLogoutError] = useState<string | null>(null);
  const menuId = useId();

  function closeNavigation() {
    setMenuOpen(false);
    setAdminOpen(false);
  }

  function toggleDisclosure() {
    if (menuOpen) {
      closeNavigation();
      return;
    }
    setMenuOpen(true);
  }

  async function onLogout() {
    setLogoutPending(true);
    setLogoutError(null);
    try {
      await endSession();
      await signOutFromGoogle();
      queryClient.removeQueries({ queryKey: AUTH_ME_KEY });
      closeNavigation();
      router.replace("/login");
      router.refresh();
    } catch {
      setLogoutError("Sign-out failed. Try again.");
      setLogoutPending(false);
    }
  }

  return (
    <nav aria-label="Primary" className="border-b border-border bg-primary">
      <div className="mx-auto flex max-w-5xl flex-wrap items-center gap-2 px-4 py-3">
        <Link href="/main" className={`${linkClass} text-base font-semibold`}>
          Kenney&apos;s Pickem
        </Link>
        <Button
          variant="nav"
          className="ml-auto md:hidden"
          aria-expanded={menuOpen}
          aria-controls={menuId}
          onClick={toggleDisclosure}
        >
          Menu
        </Button>
        <ul
          id={menuId}
          className={`${menuOpen ? "flex" : "hidden"} w-full flex-col gap-1 md:ml-auto md:flex md:w-auto md:flex-row md:items-center`}
        >
          {GAME_LINKS.map((link) => (
            <li key={link.href}>
              <Link
                href={link.href}
                className={linkClass}
                onClick={closeNavigation}
              >
                {link.label}
              </Link>
            </li>
          ))}
          {isManager ? (
            <li>
              <Menu.Root
                modal={false}
                open={adminOpen}
                onOpenChange={(nextOpen) => {
                  setAdminOpen(nextOpen);
                }}
              >
                <Menu.Trigger render={<Button variant="nav" />}>
                  Admin
                </Menu.Trigger>
                <Menu.Portal>
                  <Menu.Positioner
                    className="z-10 outline-none data-anchor-hidden:hidden"
                    sideOffset={4}
                    align="start"
                  >
                    <Menu.Popup className="max-h-[var(--available-height)] min-w-44 origin-[var(--transform-origin)] overflow-y-auto rounded-md border border-border bg-surface py-1 text-text outline-none">
                      {MANAGER_LINKS.map((link) => (
                        <Menu.LinkItem
                          key={link.href}
                          closeOnClick
                          className={menuItemClass}
                          render={
                            <Link href={link.href} onClick={closeNavigation}>
                              {link.label}
                            </Link>
                          }
                        />
                      ))}
                    </Menu.Popup>
                  </Menu.Positioner>
                </Menu.Portal>
              </Menu.Root>
            </li>
          ) : null}
          <li>
            <Button
              variant="nav"
              onClick={() => {
                void onLogout();
              }}
              disabled={logoutPending}
              focusableWhenDisabled
            >
              Logout
            </Button>
          </li>
        </ul>
        {logoutError ? (
          <p
            role="alert"
            className="w-full rounded-md bg-surface px-3 py-2 text-sm text-danger"
          >
            {logoutError}
          </p>
        ) : null}
      </div>
    </nav>
  );
}
