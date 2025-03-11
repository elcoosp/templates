# How to transform from [base repo](https://github.com/lynx-family/integrating-lynx-demo-projects) to templated

- EmptyProject -> {{project_name}} in source & file names
- emptyproject -> {{project_name  | camel_case}} in source & file names

**Using** :

```sh
find **/*emptyproject -print0 | rename -n -0 -e '$_ =~ s/emptyproject/{{project_name|camel_case}}/g'
```
