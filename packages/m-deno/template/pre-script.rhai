
const PACKAGES_ROOT_FOLDER = "packages";
const WM_PLACEHOLDER = "workspaceMember";
fn cpWmFile(wmFolderPath, filepath){
    system::command("cp", ["-R",`${global::PACKAGES_ROOT_FOLDER}/${global::WM_PLACEHOLDER}/${filepath}`, `${wmFolderPath}/${filepath}`]);
}

fn createMembersFiles(){
    let workspaceMembers = variable::get("members").split();
    variable::set("workspaceMembers", workspaceMembers);
    for (wm, i) in workspaceMembers {
        let wmFolderPath = `${global::PACKAGES_ROOT_FOLDER}/${wm}`;
        debug(`${wm} -> ${wmFolderPath}`);
        let pkgJson = `
{
    "name": "@{{scope}}/${wm}",
    "author": "{{authors}}",
    "version": "0.0.0",
    "type": "module",
    "description": "{{description}}",
    "files": ["src"],
    "publishConfig": {
      "access": "public"
    },
    "scripts": {
      "build": "echo \"Nothing to build, it's deno!\"",
      "dev": "deno run --watch src/mod.ts",
      "check": "deno check src/mod.ts",
      "lint": "deno lint",
      "fmt": "deno fmt",
      "test": "deno test -A",
      "test:cov": "deno test -A --coverage=cov/ && deno coverage --lcov cov/ > cov.lcov",
      "test:watch": "deno test --watch",
      "doc": "deno doc --html src/mod.ts"
    }
  }  
`;
        system::command("mkdir", [wmFolderPath]);
        file::write(`${wmFolderPath}/package.json`, pkgJson);
        system::command("mkdir", [`${wmFolderPath}/src`]);
        cpWmFile(wmFolderPath, "src/mod.ts");
        system::command("mkdir", [`${wmFolderPath}/tests`]);
        cpWmFile(wmFolderPath, "tests/mod.test.ts");


        // FIXME return exit status 1
        // system::command("sed", ["-i", "-e", `'s/WM/${wm}/g'`,`${wmFolderPath}/package.json`]);
        // system::command("cargo", ["generate", "--allow-commands", "-p", "../packages/mm-vite-react/template", "--name", wm, "--template-values-file", "../tests/mm-rslib.yml"]);
    }
    file::delete(`${global::PACKAGES_ROOT_FOLDER}/${global::WM_PLACEHOLDER}`)
    
}
createMembersFiles();