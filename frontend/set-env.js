// scripts/set-env.js
import { writeFileSync } from 'fs';
import { join } from 'path';

const envContent = `
export const environment = {
  production: true,
  API_URL: '${process.env.API_URL || 'http://localhost:8080'}',
  API_HOST: '${process.env.API_HOST || 'localhost:8080'}',
  UI_URL: '${process.env.UI_URL || 'http://localhost:4200'}',
  infiniteLogin: ${process.env.INFINITE_LOGIN === 'true' ? true : false},
};
`;

const targetPath = join(__dirname, '../src/environments/environment.ts');

writeFileSync(targetPath, envContent);