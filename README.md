doctrine
========

doctrine is an app to download Scala docs.

setup
-----

install conscript (`cs`).

- https://github.com/n8han/conscript/

install `doctrine` using it.

```
$ cs eed3si9n/doctrine
```

usage
-----

```
$ doctrine "org.scala-lang" % "scala-library" % "2.11.2" -o ~/doc
[info] unzippped documents to /Users/foo/doc/scala-library-2.11.2-javadoc
$ doctrine "com.github.scopt" %% "scopt" % "3.2.0" -o ~/doc      
[info] downloading https://oss.sonatype.org/content/repositories/public/com/github/scopt/scopt_2.11/3.2.0/scopt_2.11-3.2.0-javadoc.jar ...
[info]  [SUCCESSFUL ] com.github.scopt#scopt_2.11;3.2.0!scopt_2.11.jar(doc) (4525ms)
[info] unzippped documents to /Users/foo/doc/scopt_2.11-3.2.0-javadoc
```
