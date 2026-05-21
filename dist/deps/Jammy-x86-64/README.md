# Kakadu compile for Ubuntu Jammy

Ubuntu 22.04(Jammy) only provides up to GLIBCXX_3.4.30

```
apt list --installed|grep libstdc

WARNING: apt does not have a stable CLI interface. Use with caution in scripts.

libstdc++6/jammy-updates,jammy-security,now 12.3.0-1ubuntu1~22.04 amd64 [installed,automatic]
```

Ubuntu 24.04 has GLIBCXX_3.4.28. 