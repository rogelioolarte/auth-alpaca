import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const API_URL = process.env.API_URL || 'http://localhost:8080';
const API_HOST = process.env.API_HOST || 'localhost:8080';
const UI_URL = process.env.UI_URL || 'http://localhost:4200';
const infiniteLogin = process.env.INFINITE_LOGIN === 'true';

const envContent = `
export const environment = {
  production: true,
  API_URL: '${API_URL}',
  API_HOST: '${API_HOST}',
  UI_URL: '${UI_URL}',
  infiniteLogin: ${infiniteLogin},
};
`;

const targetPath = path.join(__dirname, '../src/environments/environment.prod.ts');

try {
  fs.writeFileSync(targetPath, envContent);
  console.log(`Environment file successfully generated in: ${targetPath}`);
} catch (err) {
  console.error('Error generating environment file:', err);
  process.exit(1);
}