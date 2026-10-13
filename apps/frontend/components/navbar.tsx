"use client";

import { useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useId, useRef, useState } from "react";
import { AUTH_ME_KEY, useAuth } from "./auth-provider";
import { signOutFromGoogle } from "../lib/firebase-auth";
import { GAME_LINKS, MANAGER_LINKS } from "../lib/nav-links";
import { endSession } from "../lib/session-client";

const linkClass =
  "rounded-md px-3 py-2 text-sm font-medium text-primary-fg focus-visible:outline-2 focus-visible:outline-offset-2";

export function Navbar() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { isManager } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const [adminOpen, setAdminOpen] = useState(false);
  const [logoutPending, setLogoutPending] = useState(false);
  const [logoutError, setLogoutError] = useState<string | null>(null);
  const adminRef = useRef<HTMLLIElement>(null);
  const menuId = useId();
  const adminMenuId = useId();

  useEffect(() => {
    if (!adminOpen) {
      return;
    }
    function onPointerDown(event: MouseEvent) {
      const target = event.target;
      if (!(target instanceof Node) || !adminRef.current?.contains(target)) {
        setAdminOpen(false);
      }
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setAdminOpen(false);
      }
    }
    document.addEventListener("mousedown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("mousedown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [adminOpen]);

  async function onLogout() {
    setLogoutPending(true);
    setLogoutError(null);
    try {
      await endSession();
      await signOutFromGoogle();
      queryClient.removeQueries({ queryKey: AUTH_ME_KEY });
      setMenuOpen(false);
      setAdminOpen(false);
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
        <button
          type="button"
          className={`${linkClass} ml-auto md:hidden`}
          aria-expanded={menuOpen}
          aria-controls={menuId}
          onClick={() => {
            setMenuOpen((open) => !open);
          }}
        >
          Menu
        </button>
        <ul
          id={menuId}
          className={`${menuOpen ? "flex" : "hidden"} w-full flex-col gap-1 md:ml-auto md:flex md:w-auto md:flex-row md:items-center`}
        >
          {GAME_LINKS.map((link) => (
            <li key={link.href}>
              <Link
                href={link.href}
                className={linkClass}
                onClick={() => {
                  setMenuOpen(false);
                }}
              >
                {link.label}
              </Link>
            </li>
          ))}
          {isManager ? (
            <li className="relative" ref={adminRef}>
              <button
                type="button"
                className={linkClass}
                aria-expanded={adminOpen}
                aria-haspopup="menu"
                aria-controls={adminMenuId}
                onClick={() => {
                  setAdminOpen((open) => !open);
                }}
              >
                Admin
              </button>
              {adminOpen ? (
                <ul
                  id={adminMenuId}
                  role="menu"
                  className="z-10 min-w-44 rounded-md border border-border bg-surface py-1 text-text md:absolute md:mt-1"
                >
                  {MANAGER_LINKS.map((link) => (
                    <li key={link.href} role="none">
                      <Link
                        href={link.href}
                        role="menuitem"
                        className="block px-3 py-2 text-sm text-text focus-visible:outline-2 focus-visible:outline-offset-2"
                        onClick={() => {
                          setAdminOpen(false);
                          setMenuOpen(false);
                        }}
                      >
                        {link.label}
                      </Link>
                    </li>
                  ))}
                </ul>
              ) : null}
            </li>
          ) : null}
          <li>
            <button
              type="button"
              className={linkClass}
              onClick={() => {
                void onLogout();
              }}
              disabled={logoutPending}
            >
              Logout
            </button>
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
