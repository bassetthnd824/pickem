//@ts-check

/** @type {import('next').NextConfig} */
const nextConfig = {
  // Load from node_modules in the server build. This list does not keep
  // the package out of the client bundle; the import graph does.
  serverExternalPackages: ["firebase-admin"],
};

module.exports = nextConfig;
