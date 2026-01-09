import express from 'express';

const app = express();
const port = 8082;

// In a real app, this key would be securely generated and managed.
const HARDCODED_KEY = 'a-super-secret-and-securely-generated-key';

app.use(express.json());

// Get a key for a room
app.get('/key', (req, res) => {
  // In a real app, you would have logic to determine which key to return based on the room or user.
  // You would also want to ensure that only authorized users can request keys.

  res.send({ key: HARDCODED_KEY });
});

app.listen(port, () => {
  console.log(`KMS service listening on port ${port}`);
});
