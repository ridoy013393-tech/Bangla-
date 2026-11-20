# Bangla Chat — Telegram Bot Two-Way Moderation Webhook (Free on Vercel)

This lightweight serverless function connects your Telegram Bot directly to your Bangla Chat Cloud Firestore database so you can tap **Ban / Mute (1h, 24h, 7d) / Remove / Unban** buttons right inside Telegram or run slash commands (`/users`, `/ban`, `/mute`, `/unban`, `/delete`, `/info`).

## Quick Free Deployment (Vercel)

1. Push the `telegram-bot-webhook` folder to a private GitHub repo and import it into [Vercel](https://vercel.com) (Free Hobby Plan).
2. In **Firebase Console → Project Settings → Service Accounts**, click **Generate new private key** to download your Firebase Admin service account JSON.
3. In **Vercel → Project Settings → Environment Variables**, add:
   - `TELEGRAM_BOT_TOKEN` = Your bot token from `@BotFather`
   - `TELEGRAM_ADMIN_CHAT_ID` = Your numeric Telegram Chat ID
   - `FIREBASE_SERVICE_ACCOUNT_JSON` = Paste the single-line JSON content of your Firebase service account key
   - `FIRESTORE_DATABASE_ID` = `ai-studio-android-banglach-63aa7c60-ef42-45f3-b626-422fe5d420a5`
4. Deploy on Vercel to get your URL, e.g.:
   `https://your-project.vercel.app/api/telegram`
5. Register the Telegram webhook once by opening this URL in your browser (replace `<YOUR_BOT_TOKEN>` and `<YOUR_VERCEL_URL>`):
   ```
   https://api.telegram.org/bot<YOUR_BOT_TOKEN>/setWebhook?url=https://<YOUR_VERCEL_URL>/api/telegram
   ```
6. Send `/help` to your bot in Telegram — you are live!

## Security Note
- The webhook strictly checks `chat.id === TELEGRAM_ADMIN_CHAT_ID` and ignores every message or button click from any other Telegram user.
- Never commit your `TELEGRAM_BOT_TOKEN` or Firebase Service Account JSON to any public repository.
