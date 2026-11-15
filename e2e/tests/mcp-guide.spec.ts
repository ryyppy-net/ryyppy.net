import { test, expect } from './fixtures';
import { SHARED_STORAGE_STATE } from './shared-user';

test.describe('MCP guide', () => {
  test.use({ storageState: SHARED_STORAGE_STATE });

  test('the modern UI links to the guide, which names the server address', async ({ page, baseURL }) => {
    await page.goto('/app/profile-settings', { waitUntil: 'domcontentloaded' });
    await page.locator('#mcp-link').click();

    await expect(page).toHaveURL(/\/ui\/mcp$/);
    await expect(page.locator('h1')).toBeVisible();
    await expect(page.locator('#mcpAddress')).toHaveText(`${baseURL}/mcp`);
    await expect(page.locator('#mcpAddCommand')).toContainText(`claude mcp add --transport http ryyppynet ${baseURL}/mcp`);
    expect(await page.content()).not.toContain('??');
  });

  test('the classic UI links to the guide from its settings', async ({ page }) => {
    await page.goto('/ui/user', { waitUntil: 'domcontentloaded' });
    await page.locator('#configureDrinkerButtonLink').click();
    await page.locator('#mcpLink').click();

    await expect(page).toHaveURL(/\/ui\/mcp$/);
    await expect(page.locator('#mcpAddress')).toBeVisible();
  });

  test('a signed-out visitor reaches the guide from the login page', async ({ browser }) => {
    const context = await browser.newContext({ storageState: { cookies: [], origins: [] } });
    const page = await context.newPage();
    await page.goto('/ui/login', { waitUntil: 'domcontentloaded' });
    await page.locator('a[href="/ui/mcp"]').click();
    await expect(page).toHaveURL(/\/ui\/mcp$/);
    await expect(page.locator('#mcpAddress')).toBeVisible();
    await context.close();
  });
});
