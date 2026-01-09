import express from 'express';
import jwt from 'jsonwebtoken';

const app = express();
const port = 8081;

// In a real app, this would be a secure, randomly generated string
const JWT_SECRET = process.env.JWT_SECRET || 'your-super-secret-key';

app.use(express.json());

// Generate a token for a user
app.post('/token', (req, res) => {
  const { username } = req.body;

  if (!username) {
    return res.status(400).send({ error: 'Username is required' });
  }

  // In a real app, you would first authenticate the user (e.g., with a password)

  const token = jwt.sign({ username }, JWT_SECRET, { expiresIn: '1h' });

  res.send({ token });
});

// Verify a token
app.post('/verify', (req, res) => {
  const { token } = req.body;

  if (!token) {
    return res.status(400).send({ error: 'Token is required' });
  }

  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    res.send({ valid: true, decoded });
  } catch (error) {
    res.status(401).send({ valid: false, error: 'Invalid token' });
  }
});

app.listen(port, () => {
  console.log(`Auth service listening on port ${port}`);
});
