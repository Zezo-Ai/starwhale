/*
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.starwhale.mlops.domain.dataset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.same;
import static org.mockito.Mockito.mockStatic;

import ai.starwhale.mlops.api.protocol.dataset.DatasetVersionVo;
import ai.starwhale.mlops.api.protocol.dataset.DatasetVo;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.VersionAliasConvertor;
import ai.starwhale.mlops.domain.bundle.BundleException;
import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleUrl;
import ai.starwhale.mlops.domain.bundle.BundleVersionUrl;
import ai.starwhale.mlops.domain.bundle.remove.RemoveManager;
import ai.starwhale.mlops.domain.bundle.revert.RevertManager;
import ai.starwhale.mlops.domain.dataset.bo.DatasetQuery;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersionQuery;
import ai.starwhale.mlops.domain.dataset.converter.DatasetVersionConvertor;
import ai.starwhale.mlops.domain.dataset.converter.DatasetVoConvertor;
import ai.starwhale.mlops.domain.dataset.dataloader.DataLoader;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetMapper;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetVersionMapper;
import ai.starwhale.mlops.domain.dataset.objectstore.DsFileGetter;
import ai.starwhale.mlops.domain.dataset.po.DatasetEntity;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.storage.StorageService;
import ai.starwhale.mlops.domain.trash.TrashService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Setter;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

public class DatasetServiceTest {

    private DatasetService service;
    private DatasetMapper datasetMapper;
    private DatasetVersionMapper datasetVersionMapper;
    private DatasetVoConvertor datasetConvertor;
    private DatasetVersionConvertor versionConvertor;
    private StorageService storageService;
    private ProjectManager projectManager;
    private DatasetManager datasetManager;
    private UserService userService;
    private DsFileGetter dsFileGetter;
    private DataLoader dataLoader;
    private TrashService trashService;
    @Setter
    private BundleManager bundleManager;

    @SneakyThrows
    @BeforeEach
    public void setUp() {
        datasetMapper = mock(DatasetMapper.class);
        datasetVersionMapper = mock(DatasetVersionMapper.class);
        datasetConvertor = mock(DatasetVoConvertor.class);
        given(datasetConvertor.convert(any(DatasetEntity.class)))
                .willAnswer(invocation -> {
                    DatasetEntity entity = invocation.getArgument(0);
                    return DatasetVo.builder()
                            .id(String.valueOf(entity.getId()))
                            .name(entity.getName())
                            .build();
                });
        versionConvertor = mock(DatasetVersionConvertor.class);
        given(versionConvertor.convert(any(DatasetVersionEntity.class)))
                .willAnswer(invocation -> {
                    DatasetVersionEntity entity = invocation.getArgument(0);
                    return DatasetVersionVo.builder()
                            .id(String.valueOf(entity.getId()))
                            .name(entity.getName())
                            .build();
                });

        storageService = mock(StorageService.class);
        given(storageService.listStorageFile(any()))
                .willReturn(List.of());
        given(storageService.getStorageSize(any()))
                .willReturn(1000L);

        userService = mock(UserService.class);
        given(userService.currentUserDetail())
                .willReturn(User.builder().id(1L).idTableKey(1L).build());
        projectManager = mock(ProjectManager.class);
        given(projectManager.getProjectId(same("1")))
                .willReturn(1L);
        given(projectManager.getProjectId(same("2")))
                .willReturn(2L);
        datasetManager = mock(DatasetManager.class);

        dsFileGetter = mock(DsFileGetter.class);

        dataLoader = mock(DataLoader.class);

        trashService = mock(TrashService.class);

        service = new DatasetService(
                projectManager,
                datasetMapper,
                datasetVersionMapper,
                datasetConvertor,
                versionConvertor,
                storageService,
                datasetManager,
                new IdConvertor(),
                new VersionAliasConvertor(),
                userService,
                dsFileGetter,
                dataLoader,
                trashService
        );
        bundleManager = mock(BundleManager.class);
        given(bundleManager.getBundleId(any(BundleUrl.class)))
                .willAnswer(invocation -> {
                    BundleUrl bundleUrl = invocation.getArgument(0);
                    switch (bundleUrl.getBundleUrl()) {
                        case "d1":
                            return 1L;
                        case "d2":
                            return 2L;
                        case "d3":
                            return 3L;
                        default:
                            throw new BundleException("");
                    }
                });

        given(bundleManager.getBundleVersionId(any(BundleVersionUrl.class)))
                .willAnswer((Answer<Long>) invocation -> {
                    BundleVersionUrl url = invocation.getArgument(0);
                    switch (url.getVersionUrl()) {
                        case "v1":
                            return 1L;
                        case "v2":
                            return 2L;
                        case "v3":
                            return 3L;
                        default:
                            throw new BundleException("");
                    }
                });

        service.setBundleManager(bundleManager);
    }

    @Test
    public void testListSwmp() {
        given(datasetMapper.listDatasets(same(1L), anyString()))
                .willReturn(List.of(
                        DatasetEntity.builder().id(1L).build(),
                        DatasetEntity.builder().id(2L).build()
                ));
        var res = service.listSwDataset(DatasetQuery.builder()
                .projectUrl("1")
                .namePrefix("")
                .build(), new PageParams(1, 5));
        assertThat(res, allOf(
                hasProperty("size", is(2)),
                hasProperty("list", hasItem(hasProperty("id", is("1")))),
                hasProperty("list", hasItem(hasProperty("id", is("2"))))
        ));
    }

    @Test
    public void testDeleteDataset() {
        RemoveManager removeManager = mock(RemoveManager.class);
        given(removeManager.removeBundle(argThat(
                url -> Objects.equals(url.getProjectUrl(), "p1") && Objects.equals(url.getBundleUrl(), "d1")
        ))).willReturn(true);
        try (var mock = mockStatic(RemoveManager.class)) {
            mock.when(() -> RemoveManager.create(any(), any()))
                    .thenReturn(removeManager);
            var res = service.deleteDataset(DatasetQuery.builder().projectUrl("p1").datasetUrl("d1").build());
            assertThat(res, is(true));

            res = service.deleteDataset(DatasetQuery.builder().projectUrl("p2").datasetUrl("d2").build());
            assertThat(res, is(false));
        }
    }

    @Test
    public void testGetDatasetInfo() {
        given(datasetMapper.findDatasetById(same(1L)))
                .willReturn(DatasetEntity.builder().id(1L).build());

        given(datasetMapper.findDatasetById(same(2L)))
                .willReturn(DatasetEntity.builder().id(2L).build());

        assertThrows(StarwhaleApiException.class,
                () -> service.getDatasetInfo(DatasetQuery.builder().projectUrl("1").datasetUrl("d3").build()));

        given(datasetVersionMapper.getVersionById(same(1L)))
                .willReturn(DatasetVersionEntity.builder().id(1L).versionOrder(2L).build());

        given(datasetVersionMapper.getLatestVersion(same(1L)))
                .willReturn(DatasetVersionEntity.builder().id(1L).versionOrder(2L).build());

        var res = service.getDatasetInfo(DatasetQuery.builder()
                .projectUrl("1")
                .datasetUrl("d1")
                .datasetVersionUrl("v1")
                .build());

        assertThat(res, allOf(
                hasProperty("id", is("1")),
                hasProperty("versionAlias", is("v2"))
        ));

        given(datasetVersionMapper.getLatestVersion(same(1L)))
                .willReturn(DatasetVersionEntity.builder().id(1L).versionOrder(2L).build());

        res = service.getDatasetInfo(DatasetQuery.builder()
                .projectUrl("1")
                .datasetUrl("d1")
                .build());

        assertThat(res, allOf(
                hasProperty("id", is("1")),
                hasProperty("versionAlias", is("v2"))
        ));

        assertThrows(StarwhaleApiException.class,
                () -> service.getDatasetInfo(DatasetQuery.builder().projectUrl("1").datasetUrl("d2").build()));
    }

    @Test
    public void testModifyDatasetVersion() {
        given(datasetVersionMapper.update(argThat(entity -> entity.getId() == 1L)))
                .willReturn(1);

        var res = service.modifyDatasetVersion("1", "d1", "v1", new DatasetVersion());
        assertThat(res, is(true));

        res = service.modifyDatasetVersion("1", "d1", "v2", new DatasetVersion());
        assertThat(res, is(false));
    }

    @Test
    public void testRevertVersionTo() {
        RevertManager revertManager = mock(RevertManager.class);
        given(revertManager.revertVersionTo(argThat(
                url ->
                        Objects.equals(url.getBundleUrl().getProjectUrl(), "p1")
                                && Objects.equals(url.getBundleUrl().getBundleUrl(), "m1")
                                && Objects.equals(url.getVersionUrl(), "v1")
        ))).willReturn(true);
        try (var mock = mockStatic(RevertManager.class)) {
            mock.when(() -> RevertManager.create(any(), any()))
                    .thenReturn(revertManager);

            var res = service.revertVersionTo("p1", "m1", "v1");
            assertThat(res, is(true));

            res = service.revertVersionTo("p1", "m1", "v2");
            assertThat(res, is(false));
        }
    }

    @Test
    public void testListDatasetVersionHistory() {
        given(datasetVersionMapper.listVersions(anyLong(), anyString(), anyString()))
                .willReturn(List.of(DatasetVersionEntity.builder().id(1L).datasetName("d1").build()));
        var res = service.listDatasetVersionHistory(
                DatasetVersionQuery.builder()
                        .projectUrl("1")
                        .datasetUrl("d1")
                        .versionName("v1")
                        .versionTag("tag1")
                        .build(),
                PageParams.builder().build()
        );
        assertThat(res, allOf(
                hasProperty("list", iterableWithSize(1))
        ));
    }

    @Test
    public void testFindDatasetByVersionIds() {
        given(datasetVersionMapper.findVersionsByIds(anyList()))
                .willReturn(List.of(
                        DatasetVersionEntity.builder().datasetId(1L).build()
                ));

        given(datasetMapper.findDatasetById(same(1L)))
                .willReturn(DatasetEntity.builder().id(1L).build());

        given(datasetMapper.findDatasetsByIds(anyList()))
                .willAnswer(invocation -> {
                    List<Long> ids = invocation.getArgument(0);
                    return ids.stream()
                            .map(id -> DatasetEntity.builder().id(id).build())
                            .collect(Collectors.toList());
                });

        var res = service.findDatasetsByVersionIds(List.of());
        assertThat(res, allOf(
                iterableWithSize(1),
                hasItem(hasProperty("id", is("1")))
        ));
    }

    @Test
    public void testListModelInfo() {
        given(datasetMapper.findByName(same("d1"), same(1L)))
                .willReturn(DatasetEntity.builder().id(1L).build());
        given(datasetVersionMapper.listVersions(same(1L), any(), any()))
                .willReturn(List.of(DatasetVersionEntity.builder().versionOrder(2L).build()));

        var res = service.listDs("1", "d1");
        assertThat(res, hasItem(allOf(
                hasProperty("id", is("1")),
                hasProperty("versionAlias", is("v2"))
        )));

        given(projectManager.findByNameOrDefault(same("1"), same(1L)))
                .willReturn(ProjectEntity.builder().id(1L).build());
        given(datasetMapper.listDatasets(same(1L), any()))
                .willReturn(List.of(DatasetEntity.builder().id(1L).build()));

        res = service.listDs("1", "");
        assertThat(res, hasItem(allOf(
                hasProperty("id", is("1")),
                hasProperty("versionAlias", is("v2"))
        )));

        assertThrows(SwValidationException.class,
                () -> service.listDs("2", "d1"));
    }

    @Test
    public void testQuery() {
        given(datasetVersionMapper.getVersionById(same(1L)))
                .willReturn(DatasetVersionEntity.builder().id(1L).build());
        var res = service.query("1", "d1", "v1");
        assertThat(res, hasProperty("id", is(1L)));

        assertThrows(StarwhaleApiException.class,
                () -> service.query("1", "d1", "v2"));

    }

    @Test
    public void testDataOf() {
        given(dsFileGetter.dataOf(same(1L), anyString(), any(), any()))
                .willReturn(new byte[1]);

        var res = dsFileGetter.dataOf(1L, "", 1L, 1L);
        assertThat(res, notNullValue());
    }

    @Test
    public void testLinkOf() {
        given(dsFileGetter.linkOf(same(1L), anyString(), anyLong()))
                .willReturn("link");

        var res = dsFileGetter.linkOf(1L, "", 1L);
        assertThat(service.signLink(1L, "", 1L), is("link"));
    }

    @Test
    public void testLinksOf() {
        given(dsFileGetter.linkOf(same(1L), eq("a"), anyLong()))
                .willReturn("link1");

        given(dsFileGetter.linkOf(same(1L), eq("b"), anyLong()))
                .willReturn("link2");
        given(dsFileGetter.linkOf(same(1L), eq("x"), anyLong()))
                .willThrow(SwValidationException.class);

        Assertions.assertEquals(Map.of("a", "link1", "b", "link2", "x", ""),
                service.signLinks(1L, Set.of("a", "b", "x"), 1L));
    }

}