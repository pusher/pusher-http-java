# Release process

This documents our release process for the library

## Prerequisites

You will need a Java 6 JDK. OS X should prompt you to install one
automatically at some point in this process.

The library build and deploy process is managed by Maven:

    $ brew install maven

GNUPG is required to sign releases if they are to be published

    $ brew install gnupg

## Preparing for development

If you want to work on the code in Eclipse, the following will generate config
so that you can import it as an existing Eclipse project

    $ mvn eclipse:clean eclipse:eclipse

## Building

Clean the output directory (not usually necessary, but can avoid some odd
situations)

    $ mvn clean

Build the code and run the test suite

    $ mvn test

Build the code, run the tests, package and install into local maven repo (if
you’re developing something which depends on the library and have made
unreleased changes to lib which you want access to in your project).

    $ mvn install

## Release / Deploy (push to global repository)

### Maven settings

We deploy to the Sonatype repository, from there is is made available via the
global Maven infrastructure. We have an account on their JIRA issue tracking
system (issues.sonatype.org) where we can file administrative requests if we
need to.

To deploy to the repositories, you need the following in your `~/.m2/settings.xml`

```xml
<settings>
  <servers>

    <server>
      <id>sonatype-nexus-snapshots</id>
      <username>pusher</username>
      <password>***password***</password>
    </server>

    <server>
      <id>sonatype-nexus-staging</id>
      <username>pusher</username>
      <password>***password***</password>
    </server>

  </servers>
</settings>
```

The password-in-file situation is hardly ideal, but as far as I’m aware it’s
not avoidable. It can be obfuscated (see
[here](http://maven.apache.org/guides/mini/guide-encryption.html)), but
prompting for entry when it’s required is not supported.

### GPG signing

All the uploaded releases (not snapshots) must be signed with a key which has
been submitted to the [MIT keyserver](http://pgp.mit.edu/). Both public and
private parts of a key generated for `Pusher <support@pusher.com>` need to be
imported to your keychain.

In addition, **if you have existing gpg keys** you must set this key as the
default by adding 

    default-key C68187EB

to your `~/.gnupg/gpg.conf` file, or the first key in your keyring will be
used.

The build will prompt for the key passphrase when it comes to signing the
package.

### Uploading a snapshot

If you want to share a pre-release version with someone else, you can publish
a snapshot (maven jargon for pre-release version: "1.0-SNAPSHOT" is anything
built from the pre-release code leading up to the "1.0" release. In the
repository, timestamps are automatically added to the version to differentiate
between builds

    $ mvn clean deploy

Other people can now depend on this in their builds, if they add the snapshot
repository to their build. Note though that maven will always pull the latest
snapshot, you can’t depend on a particular build. You can share the http links
to the actual jar files though.

I can’t see us doing much with snapshots.

### Building a release

#### A note on versioning

Generally, the version recorded in the pom.xml (maven project definition)
should be of the form

    <next-version-number>-SNAPSHOT

Any pre-release builds from this state are considered a "snapshot" of the
state as the release number in question approaches.

It is important that this is the case when one comes to do a release. The
maven release process will prompt (with sensible defaults) for various version
related questions during the prepare stage and manipulate the pom
appropriately.

#### The release process

Building a release is a two part process

    $ mvn release:prepare

The code will be built and the tests run. The plugin will prompt for three
version strings:

1. The version number you intend to release. Unless you’re releasing some sort
   of beta, this should be the current development version without the
   `-SNAPSHOT` suffix, and will default to this.

2. The git tag name with which to mark the release code. Defaults to
   `<project-name>-<release version>` which should be fine.

3. The version of the next development code. This should end -SNAPSHOT, and
   will generally just be the version you’re releasing with an incremented
   patch version. The default should be appropriate unless you chose more
   detail in step 1 (e.g. by default 1.0.1-beta-SNAPSHOT follows 1.0.0-beta,
   but what you probably want is 1.0.0-SNAPSHOT again)

A tag will be created from the HEAD of your current local branch (which must
be synced with origin)

NOTE: Maven will commit changes to the project pom.xml, create a tag and push
them to github during this process.

After a successful prepare, invoke

    $ mvn release:perform

This will make a fresh checkout of the tag created in the prepare stage from
github, build it and upload the resulting packages to the sonatype maven
repository, push the new javadocs to the github pages etc.

#### Promoting a release

Now that the release is built and pushed to the sonatype "staging" area, log
in to "[Nexus](https://oss.sonatype.org)", choose
staging repositories, use the filter in the top right to locate the one you
have just created. Select it and click "Close". Some quality checks required
for inclusion in Maven central will be run and after a minute or so the
staging repository will be marked as "closed", at which point you can select
it and click "Release". Congratulations, the (rather convoluted) process is
complete, and the release will shortly be available in the default global
repo.

There’s a more detailed description of the intricacies of the Nexus UI in the
[Sonatype howto](https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide#SonatypeOSSMavenRepositoryUsageGuide-8a.ReleaseIt) 

