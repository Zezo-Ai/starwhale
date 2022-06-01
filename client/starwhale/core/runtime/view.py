import typing as t
import os
from pathlib import Path
from starwhale.core.runtime.store import RuntimeStorage

from starwhale.consts import (
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    DEFAULT_PYTHON_VERSION,
    DefaultYAMLName,
    PythonRunEnv,
)
from starwhale.base.type import URIType
from starwhale.base.uri import URI
from starwhale.base.view import BaseTermView
from starwhale.utils import console, in_production

from .model import Runtime


class RuntimeTermView(BaseTermView):
    def __init__(self, runtime_uri: str) -> None:
        super().__init__()

        self.raw_uri = runtime_uri
        self.uri = URI(runtime_uri, expected_type=URIType.RUNTIME)
        self.runtime = Runtime.get_runtime(self.uri)

    @BaseTermView._simple_action_print
    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        return self.runtime.remove(force)

    @BaseTermView._simple_action_print
    def recover(self, force: bool = False) -> t.Tuple[bool, str]:
        return self.runtime.recover(force)

    @BaseTermView._header
    def history(self, fullname: bool = False) -> None:
        self._print_history(
            title="Runtime History", history=self.runtime.history(), fullname=fullname
        )

    @BaseTermView._header
    def info(self, fullname: bool = False) -> None:
        self._print_info(self.runtime.info(), fullname=fullname)

    @classmethod
    def build(
        cls,
        workdir: str,
        project: str = "",
        yaml_name: str = DefaultYAMLName.RUNTIME,
        gen_all_bundles: bool = False,
    ) -> None:
        _runtime_uri = cls.prepare_build_bundle(
            workdir, project, yaml_name, URIType.MODEL
        )
        _rt = Runtime.get_runtime(_runtime_uri)
        _rt.build(Path(workdir), yaml_name, gen_all_bundles=gen_all_bundles)

    def extract(self, force: bool = False, target: t.Union[str, Path] = "") -> None:
        console.print(":oncoming_police_car: try to extract ...")
        path = self.runtime.extract(force, target)
        console.print(f":clap: extracted @ {path.resolve()} :tada:")

    @classmethod
    @BaseTermView._pager
    @BaseTermView._header
    def list(
        cls,
        project_uri: str = "",
        fullname: bool = False,
        show_removed: bool = False,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.Dict[str, t.Any], t.Dict[str, t.Any]]:
        _uri = URI(project_uri, expected_type=URIType.PROJECT)
        _runtimes, _pager = Runtime.list(_uri, page, size)
        BaseTermView._print_list(_runtimes, show_removed, fullname)
        return _runtimes, _pager

    @classmethod
    def create(
        cls,
        workdir: str,
        name: str,
        python_version: str = DEFAULT_PYTHON_VERSION,
        mode: str = PythonRunEnv.VENV,
        force: bool = False,
    ) -> None:
        console.print(f":construction: start to create runtime[{name}] environment...")
        Runtime.create(
            workdir, name, python_version=python_version, mode=mode, force=force
        )
        console.print(":clap: python runtime environment is ready to use :tada:")

    @classmethod
    def restore(cls, target: str) -> None:
        if in_production() or (os.path.exists(target) and os.path.isdir(target)):
            workdir = Path(target)
        else:
            uri = URI(target, URIType.RUNTIME)
            store = RuntimeStorage(uri)
            workdir = store.snapshot_workdir

        console.print(f":golfer: try to restore python runtime environment{workdir}...")
        Runtime.restore(workdir)

    @classmethod
    def copy(cls, src_uri: str, dest_uri: str, force: bool = False) -> None:
        Runtime.copy(src_uri, dest_uri, force)
        console.print(":clap: copy done.")