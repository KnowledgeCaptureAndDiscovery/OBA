## Authentication using Firebase

1. Go to [https://console.firebase.google.com/u/0/](https://console.firebase.google.com/u/0/)
2. Create a new project
3. Enable the authentication and copy the Web API key
    <video width="640" height="480" controls>
    <source src="../figures/firebase.mp4" type="video/mp4">
    Your browser does not support the video tag.
    </video>
4. Go to the OBA configuration file.
```yaml
auth:
  provider: firebase
firebase:
  key: YOUR_KEY
```

And re-run oba.