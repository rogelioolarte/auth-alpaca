import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const envContent = `
export const environment = {
  production: true,
  API_URL: '${process.env.API_URL || 'http://localhost:8080'}',
  UI_URL: '${process.env.UI_URL || 'http://localhost:4200'}',
  infiniteLogin: ${process.env.INFINITE_LOGIN === 'true'},
};
`;

const targetDir = path.join(__dirname, 'src/environments');
const targetPath = path.join(targetDir, 'environment.ts');

try {
  if (!fs.existsSync(targetDir)) {
    fs.mkdirSync(targetDir, { recursive: true });
  }

  fs.writeFileSync(targetPath, envContent);
  console.log(`Environment file successfully generated in: ${targetPath}`);
} catch (err) {
  console.error('Error generating environment file:', err);
  process.exit(1);
}