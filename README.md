# NeoTree Android Client

## Configuration

### App package name

```
org.neotree
```

### Crashlytics
Create an account and app on [fabric.io](https://fabric.io), add a new app configuration then create a new **app/fabric.properties** with the following content

```
apiSecret=<your application secret>
```

### Development

1) Create development signing certificate

```
keytool -genkey -v -keystore app/certs/neotree-debug.keystore \
	-alias androiddebugkey -storepass android -keypass android \
	-keyalg RSA -keysize 2048 -validity 10000
```

2) Create a firebase project for development and copy the google-services.json file into **app/src/debug**

### Release

1) Export build environment variables (there are required for each staging/release build)

```
export NEOTREE_KEYSTORE_PASSWORD=<your keystore password here>
export NEOTREE_KEY_ALIAS==<your key alias here>
export NEOTREE_KEY_PASSWORD==<your key password here>
```

2) Create staging/release signing certificate

```
keytool -genkey -v -keystore app/certs/neotree-release.keystore \
	-alias $NEOTREE_KEY_ALIAS \
	-storepass $NEOTREE_KEYSTORE_PASSWORD \
	-keypass $NEOTREE_KEY_PASSWORD \
	-keyalg RSA -keysize 2048 -validity 10000
```

3) Create a firebase project for release and copy the google-services.json file into **app/src/staging** and **app/src/release**
