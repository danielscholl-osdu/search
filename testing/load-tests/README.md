Copyright 2017-2019, Schlumberger

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

# Set the Google project ID, this should match the identity and storage service project belongs to:
    
```
$ gcloud config set project <YOUR-PROJECT-ID>
```

# Perform a basic authentication in the selected project
```
$ gcloud auth application-default login
```

# Change the folder to loadTest parent folder
```
cd load-tests/
``` 

# setup
```
pip install -r requirements.txt
virtualenv venv
./venv/scripts/activate.bat
```

# bash
```
pip install -r requirements.txt

```

# running
```
./load_tets_runner.sh
```
